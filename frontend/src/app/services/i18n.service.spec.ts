import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { TranslateService } from '@ngx-translate/core';
import { I18nService } from './i18n.service';

describe('I18nService', () => {
  let translate: {
    setDefaultLang: ReturnType<typeof vi.fn>;
    addLangs: ReturnType<typeof vi.fn>;
    use: ReturnType<typeof vi.fn>;
    get: ReturnType<typeof vi.fn>;
    instant: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    localStorage.clear();
    translate = {
      setDefaultLang: vi.fn(),
      addLangs: vi.fn(),
      use: vi.fn(),
      get: vi.fn().mockReturnValue(of('translated')),
      instant: vi.fn().mockReturnValue('translated-now'),
    };
  });

  afterEach(() => {
    localStorage.clear();
  });

  function configure(platformId: Object = 'browser' as Object): I18nService {
    TestBed.configureTestingModule({
      providers: [
        I18nService,
        { provide: TranslateService, useValue: translate },
        { provide: PLATFORM_ID, useValue: platformId },
      ],
    });
    return TestBed.inject(I18nService);
  }

  it('uses stored supported language on browser initialization', () => {
    localStorage.setItem('language', 'en');

    const service = configure();

    expect(translate.setDefaultLang).toHaveBeenCalledWith('es');
    expect(translate.addLangs).toHaveBeenCalledWith(['es', 'en']);
    expect(translate.use).toHaveBeenCalledWith('en');
    expect(service.getLanguage()).toBe('en');
  });

  it('ignores unsupported language changes', () => {
    const service = configure();
    const before = service.getLanguage();

    service.setLanguage('pt');

    expect(service.getLanguage()).toBe(before);
    expect(localStorage.getItem('language')).toBe(before);
  });

  it('does not touch localStorage or translate.use during server-side initialization', () => {
    const service = configure('server');

    expect(service.getLanguage()).toBe('es');
    expect(translate.use).not.toHaveBeenCalled();
    expect(localStorage.getItem('language')).toBeNull();
  });
});
