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
  let clubService: { createClub: ReturnType<typeof vi.fn>; uploadCover: ReturnType<typeof vi.fn> };
  let component: AddClubComponent;

  beforeEach(() => {
    router = { navigate: vi.fn().mockResolvedValue(true) };
    clubService = { createClub: vi.fn(), uploadCover: vi.fn() };

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

  it('description is required (shown only after a submit attempt, like register.page)', () => {
    fillValidForm();
    const description = component.clubForm.get('description');
    description?.setValue('');

    // Antes de intentar enviar, no se muestra (mismo comportamiento que registro).
    expect(component.showError('description')).toBe(false);

    component.onSubmit();

    expect(description?.hasError('required')).toBe(true);
    expect(component.showError('description')).toBe(true);
    expect(component.getError('description')).toBe('clubNew.errors.required');
  });

  it('cbu is required (shown only after a submit attempt, like register.page)', () => {
    fillValidForm();
    const cbu = component.clubForm.get('cbu');
    cbu?.setValue('');

    expect(component.showError('cbu')).toBe(false);

    component.onSubmit();

    expect(cbu?.hasError('required')).toBe(true);
    expect(component.showError('cbu')).toBe(true);
    expect(component.getError('cbu')).toBe('clubNew.errors.required');
  });

  it('error stays frozen until the next submit attempt, same as register.page', () => {
    clubService.createClub.mockReturnValue(of({}));
    fillValidForm();
    component.clubForm.get('name')?.setValue('');

    component.onSubmit();
    expect(component.showError('name')).toBe(true);

    // Se corrige el campo pero todavia no se reintenta el submit: el error
    // sigue visible (congelado) hasta el proximo intento, igual que en
    // register.page.
    component.clubForm.get('name')?.setValue('Fixed name');
    expect(component.showError('name')).toBe(true);

    component.onSubmit();
    expect(component.showError('name')).toBe(false);
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

  it('does not upload a cover when no image was selected', () => {
    clubService.createClub.mockReturnValue(of({ clubId: 'club-1', links: {} }));
    fillValidForm();

    component.onSubmit();

    expect(clubService.uploadCover).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/clubs']);
  });

  it('uploads the selected cover image after the club is created, using the returned id and links', () => {
    const club = { clubId: 'club-1', links: { cover: '/api/clubs/club-1/cover' } };
    clubService.createClub.mockReturnValue(of(club));
    clubService.uploadCover.mockReturnValue(of({}));
    fillValidForm();
    const file = new File(['fake-image'], 'cover.png', { type: 'image/png' });
    component.selectedImage = file;

    component.onSubmit();

    expect(clubService.uploadCover).toHaveBeenCalledWith('club-1', file, club.links);
    expect(router.navigate).toHaveBeenCalledWith(['/clubs']);
  });

  it('still navigates away when the club was created but the cover upload fails', () => {
    const club = { clubId: 'club-1', links: {} };
    clubService.createClub.mockReturnValue(of(club));
    clubService.uploadCover.mockReturnValue(throwError(() => new Error('upload failed')));
    fillValidForm();
    component.selectedImage = new File(['fake-image'], 'cover.png', { type: 'image/png' });

    component.onSubmit();

    expect(router.navigate).toHaveBeenCalledWith(['/clubs']);
    expect(component.submitting).toBe(false);
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
