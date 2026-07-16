import { convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { ClubHandlerPage } from './club-handler.page';

describe('ClubHandlerPage saveEvent', () => {
  let clubService: {
    createEvent: ReturnType<typeof vi.fn>;
    updateEvent: ReturnType<typeof vi.fn>;
    uploadEventPhoto: ReturnType<typeof vi.fn>;
    getEvents: ReturnType<typeof vi.fn>;
  };
  let reviewService: { listByClub: ReturnType<typeof vi.fn> };
  let component: ClubHandlerPage;

  const futureDate = '2099-01-01';

  const createdEvent = {
    eventId: 'event-1',
    eventName: 'Fiesta',
    description: 'desc',
    date: futureDate,
    photoId: null,
    links: {},
  } as any;

  beforeEach(() => {
    clubService = {
      createEvent: vi.fn(),
      updateEvent: vi.fn(),
      uploadEventPhoto: vi.fn(),
      getEvents: vi.fn().mockReturnValue(of([])),
    };
    reviewService = { listByClub: vi.fn().mockReturnValue(of({ reviews: [], total: 0 })) };

    component = new ClubHandlerPage(
      { queryParamMap: of(convertToParamMap({})) } as any,
      { navigate: vi.fn() } as any,
      clubService as any,
      reviewService as any,
      { detectChanges: vi.fn(), markForCheck: vi.fn() } as any,
      { instant: (key: string) => key, getLanguage: () => 'es' } as any,
      { run: (fn: () => void) => fn() } as any
    );
    component.clubId = 'club-1';
    component.eventForm = { eventName: 'Fiesta', description: 'desc', date: futureDate };
  });

  it('creates an event without calling uploadEventPhoto when no photo was selected', () => {
    clubService.createEvent.mockReturnValue(of(createdEvent));

    component.saveEvent();

    expect(clubService.createEvent).toHaveBeenCalled();
    expect(clubService.uploadEventPhoto).not.toHaveBeenCalled();
    expect(component.actionSuccessMessage).toBe('clubHandler.messages.eventSaved');
  });

  // Bug real de esta sesion: al crear un evento con foto, el handler de exito
  // usaba el "savedEvent" original (de antes de subir la foto, con
  // photoId=null) en vez del evento que devuelve uploadEventPhoto (con el
  // photoId ya seteado). La foto recien subida no se mostraba hasta el
  // proximo loadEvents().
  it('upserts the event returned by uploadEventPhoto (with photoId set), not the stale pre-upload event', () => {
    const file = new File(['fake-image'], 'foto.png', { type: 'image/png' });
    component.selectedEventPhotoFile = file;

    const eventWithPhoto = { ...createdEvent, photoId: 'photo-1' };
    clubService.createEvent.mockReturnValue(of(createdEvent));
    clubService.uploadEventPhoto.mockReturnValue(of(eventWithPhoto));

    const upsertSpy = vi.spyOn(component as unknown as { upsertEventInList: (e: unknown) => void }, 'upsertEventInList');

    component.saveEvent();

    expect(clubService.uploadEventPhoto).toHaveBeenCalledWith('club-1', 'event-1', file, createdEvent);
    expect(upsertSpy).toHaveBeenCalledWith(eventWithPhoto);
    expect(component.actionSuccessMessage).toBe('clubHandler.messages.eventSaved');
  });

  it('still saves the event with a distinct message when the photo upload fails', () => {
    const file = new File(['fake-image'], 'foto.png', { type: 'image/png' });
    component.selectedEventPhotoFile = file;

    clubService.createEvent.mockReturnValue(of(createdEvent));
    clubService.uploadEventPhoto.mockReturnValue(throwError(() => new Error('upload failed')));

    const upsertSpy = vi.spyOn(component as unknown as { upsertEventInList: (e: unknown) => void }, 'upsertEventInList');

    component.saveEvent();

    expect(upsertSpy).toHaveBeenCalledWith(createdEvent);
    expect(component.actionSuccessMessage).toBe('clubHandler.messages.eventSavedPhotoFailed');
  });

  it('rejects a past date without calling the service', () => {
    component.eventForm = { eventName: 'Fiesta', description: 'desc', date: '2000-01-01' };

    component.saveEvent();

    expect(clubService.createEvent).not.toHaveBeenCalled();
    expect(component.showEventError('date')).toBe(true);
  });
});
