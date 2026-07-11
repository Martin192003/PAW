import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AddClubComponent } from './club-new.page';
import { ClubService } from '../services/club.service';
import { I18nService } from '../services/i18n.service';

describe('AddClubComponent', () => {
  let router: { navigate: ReturnType<typeof vi.fn> };
  let clubService: { createClub: ReturnType<typeof vi.fn> };
  let component: AddClubComponent;

  beforeEach(() => {
    router = { navigate: vi.fn().mockResolvedValue(true) };
    clubService = { createClub: vi.fn() };

    TestBed.configureTestingModule({
      imports: [AddClubComponent, TranslateModule.forRoot()],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({}), paramMap: convertToParamMap({}) },
            queryParamMap: of(convertToParamMap({})),
            paramMap: of(convertToParamMap({})),
          },
        },
        { provide: Router, useValue: router as unknown as Router },
        { provide: ClubService, useValue: clubService as unknown as ClubService },
        { provide: I18nService, useValue: { instant: (key: string, params?: Record<string, unknown>) => params ? `${key}:${JSON.stringify(params)}` : key } },
      ],
    });

    component = TestBed.createComponent(AddClubComponent).componentInstance;
    component.ngOnInit();
    vi.spyOn(component as unknown as { showMap: () => void }, 'showMap').mockImplementation(() => undefined);
  });

  afterEach(() => {
    component?.ngOnDestroy();
  });

  function fillValidForm(): void {
    component.clubForm.setValue({
      name: 'Club Name',
      location: 'AB',
      priceRange: 'MEDIO',
      description: ' description ',
      musicGenre: 'TECHNO',
      zone: 'ZONA_CABA',
      isBar: 'true',
      cbu: ' 1234567890123456789012 ',
      image: null,
    });
  }

  it('description is required', () => {
    fillValidForm();
    const description = component.clubForm.get('description');

    description?.setValue('');
    description?.markAsTouched();

    expect(description?.hasError('required')).toBe(true);
    expect(component.showError('description')).toBe(true);
    expect(component.getError('description')).toBe('clubNew.errors.required');
  });

  it('cbu is required', () => {
    fillValidForm();
    const cbu = component.clubForm.get('cbu');

    cbu?.setValue('');
    cbu?.markAsTouched();

    expect(cbu?.hasError('required')).toBe(true);
    expect(component.showError('cbu')).toBe(true);
    expect(component.getError('cbu')).toBe('clubNew.errors.required');
  });

  it('cbu max length is enforced', () => {
    fillValidForm();
    const cbu = component.clubForm.get('cbu');

    cbu?.setValue('1'.repeat(51));

    expect(cbu?.hasError('maxlength')).toBe(true);
    expect(component.clubForm.invalid).toBe(true);
  });

  it('submit is blocked while invalid', () => {
    component.onSubmit();

    expect(component.clubForm.invalid).toBe(true);
    expect(clubService.createClub).not.toHaveBeenCalled();
  });

  it('valid submit trims description and cbu payload fields', () => {
    clubService.createClub.mockReturnValue(of({}));
    fillValidForm();

    component.onSubmit();

    expect(clubService.createClub).toHaveBeenCalledWith(expect.objectContaining({
      clubName: 'Club Name',
      location: 'AB',
      description: 'description',
      cbu: '1234567890123456789012',
      bar: true,
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/clubs']);
  });

  it('server field errors are displayed when provided', () => {
    clubService.createClub.mockReturnValue(
      throwError(() => new HttpErrorResponse({
        status: 400,
        statusText: 'Bad Request',
        error: { fieldErrors: { cbu: 'CBU inválido' } },
      }))
    );
    fillValidForm();

    component.onSubmit();

    expect(component.showError('cbu')).toBe(true);
    expect(component.getError('cbu')).toBe('CBU inválido');
  });
});
