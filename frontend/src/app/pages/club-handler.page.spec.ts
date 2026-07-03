import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ClubHandlerPage } from './club-handler.page';

describe('ClubHandlerPage club edition flow', () => {
  let clubService: {
    updateClub: ReturnType<typeof vi.fn>;
    getClubById: ReturnType<typeof vi.fn>;
    getCoverUrl: ReturnType<typeof vi.fn>;
  };
  let component: ClubHandlerPage;

  const club = {
    clubId: 'club-1',
    clubName: 'Club One',
    location: 'Palermo',
    priceRange: 'MEDIO',
    description: 'Original description',
    musicGenre: 'TECHNO',
    zone: 'ZONA_CABA',
    bar: true,
    cbu: '1234567890123456789012',
    rating: 4,
    quantityRatings: 10,
    createdAt: '2026-07-03T12:00:00Z',
  } as any;

  const updatedClub = {
    ...club,
    clubName: 'Updated Club',
    description: 'Updated description',
    cbu: '0000000000000000000000',
  } as any;

  beforeEach(() => {
    clubService = {
      updateClub: vi.fn(),
      getClubById: vi.fn().mockReturnValue(of(club)),
      getCoverUrl: vi.fn().mockReturnValue('/cover.png'),
    };

    component = new ClubHandlerPage(
      {} as any,
      { navigate: vi.fn().mockResolvedValue(true) } as any,
      clubService as any,
      {} as any,
      { detectChanges: vi.fn(), markForCheck: vi.fn() } as any,
      { instant: (key: string) => key } as any
    );
    component.clubId = 'club-1';
  });

  function fillValidEditForm(): void {
    component.clubForm = {
      clubName: ' Updated Club ',
      location: ' Palermo ',
      priceRange: 'ALTO',
      description: ' Updated description ',
      musicGenre: 'POP',
      zone: 'ZONA_NORTE',
      isBar: false,
      cbu: ' 0000000000000000000000 ',
    };
  }

  it('populates the edit form from the current club and enters edit mode', () => {
    component.club = club;
    component.actionErrorMessage = 'previous error';
    component.actionSuccessMessage = 'previous success';

    component.startEditClub();

    expect(component.editingClub).toBe(true);
    expect(component.clubForm).toEqual({
      clubName: 'Club One',
      location: 'Palermo',
      priceRange: 'MEDIO',
      description: 'Original description',
      musicGenre: 'TECHNO',
      zone: 'ZONA_CABA',
      isBar: true,
      cbu: '1234567890123456789012',
    });
    expect(component.actionErrorMessage).toBe('');
    expect(component.actionSuccessMessage).toBe('');
  });

  it('blocks saveClub when required trimmed fields are invalid', () => {
    fillValidEditForm();
    component.clubForm.description = '   ';
    component.actionSuccessMessage = 'previous success';

    component.saveClub();

    expect(clubService.updateClub).not.toHaveBeenCalled();
    expect(component.actionErrorMessage).toBe('clubHandler.messages.clubRequiredFields');
    expect(component.actionSuccessMessage).toBe('');
  });

  it('sends a trimmed payload to updateClub', () => {
    clubService.updateClub.mockReturnValue(of(updatedClub));
    fillValidEditForm();

    component.saveClub();

    expect(clubService.updateClub).toHaveBeenCalledWith('club-1', {
      clubName: 'Updated Club',
      location: 'Palermo',
      priceRange: 'ALTO',
      description: 'Updated description',
      musicGenre: 'POP',
      zone: 'ZONA_NORTE',
      isBar: false,
      cbu: '0000000000000000000000',
    });
  });

  it('updates UI state after a successful club update', () => {
    clubService.updateClub.mockReturnValue(of(updatedClub));
    component.editingClub = true;
    component.actionErrorMessage = 'previous error';
    fillValidEditForm();

    component.saveClub();

    expect(component.club).toBe(updatedClub);
    expect(component.editingClub).toBe(false);
    expect(component.savingClub).toBe(false);
    expect(component.actionSuccessMessage).toBe('clubHandler.messages.clubUpdated');
    expect(component.actionErrorMessage).toBe('');
  });

  it('shows an error and reloads the club when update fails', () => {
    clubService.updateClub.mockReturnValue(throwError(() => new Error('boom')));
    component.actionSuccessMessage = 'previous success';
    fillValidEditForm();

    component.saveClub();

    expect(clubService.getClubById).toHaveBeenCalledWith('club-1');
    expect(component.savingClub).toBe(false);
    expect(component.actionErrorMessage).toBe('clubHandler.messages.clubUpdateFailed');
    expect(component.actionSuccessMessage).toBe('');
  });
});
