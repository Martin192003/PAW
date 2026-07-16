import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { EventService } from './event.service';
import { ClubEvent } from '../models/ClubEvent.model';

// event.service.ts no tenia ningun test (ver AUDITORIA_ENTREGA_FINAL.md punto 25):
// mapea DTOs a ClubEvent, extrae el clubId del link "club", y resuelve las URLs de
// mutacion a partir de los links HATEOAS del evento (con fallback a una URL armada a
// mano cuando todavia no se cargó el evento completo).
describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  const clubId = 'club-1';
  const eventId = 'event-1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EventService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getEventsByClub maps DTOs to ClubEvent and extracts clubId from the "club" link', () => {
    let result: ClubEvent | undefined;
    service.getEventsByClub(clubId, 0, 20).subscribe(page => { result = page.content[0]; });

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/events`));
    req.flush([{
      id: eventId,
      eventName: 'Fiesta',
      description: 'desc',
      date: '2026-08-01',
      photoId: null,
      links: { self: `/api/clubs/${clubId}/events/${eventId}`, club: `/api/clubs/${clubId}`, photo: `/api/clubs/${clubId}/events/${eventId}/photo` },
    }]);

    expect(result?.eventId).toBe(eventId);
    expect(result?.name).toBe('Fiesta');
    expect(result?.clubId).toBe(clubId);
    expect(result?.photoUrl).toBe(`/api/clubs/${clubId}/events/${eventId}/photo`);
  });

  it('getUpcomingEvents supports both raw-array and {content:[...]} response shapes', () => {
    let total: number | undefined;
    service.getUpcomingEvents(0, 20).subscribe(page => { total = page.totalElements; });

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/events'));
    req.flush({ content: [{ id: eventId, eventName: 'Fiesta', description: 'd', date: '2026-08-01', photoId: null }] });

    expect(total).toBe(1);
  });

  it('getEventById maps a single event DTO', () => {
    let result: ClubEvent | undefined;
    service.getEventById(clubId, eventId).subscribe(event => { result = event; });

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}`));
    req.flush({ id: eventId, eventName: 'Fiesta', description: 'desc', date: '2026-08-01', photoId: 'photo-1' });

    expect(result?.eventId).toBe(eventId);
    expect(result?.photoId).toBe('photo-1');
  });

  it('createEvent falls back to a constructed URL when no club links are given', () => {
    service.createEvent(clubId, { eventName: 'Fiesta', description: 'd', date: '2026-08-01' }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith(`/clubs/${clubId}/events`));
    req.flush({ id: eventId, eventName: 'Fiesta', description: 'd', date: '2026-08-01', photoId: null });
  });

  it('createEvent uses the "events" link from the club when available', () => {
    service.createEvent(clubId, { eventName: 'Fiesta', description: 'd', date: '2026-08-01' }, {
      events: `/api/clubs/${clubId}/events`,
    }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith(`/clubs/${clubId}/events`));
    req.flush({ id: eventId, eventName: 'Fiesta', description: 'd', date: '2026-08-01', photoId: null });
  });

  it('deleteEvent uses the event\'s own "delete" link when the event is already loaded', () => {
    const loadedEvent = {
      eventId, name: 'Fiesta', description: 'd', date: '2026-08-01', photoId: '',
      links: { delete: `/api/clubs/${clubId}/events/${eventId}` },
    } as unknown as ClubEvent;

    service.deleteEvent(clubId, eventId, loadedEvent).subscribe();

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}`));
    req.flush(null);
  });

  it('deleteEvent falls back to a constructed URL when no event links are given', () => {
    service.deleteEvent(clubId, eventId).subscribe();

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}`));
    req.flush(null);
  });

  it('getEventPhoto requests a blob from the resolved photo URL', () => {
    let result: Blob | undefined;
    service.getEventPhoto(clubId, eventId).subscribe(blob => { result = blob; });

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}/photo`));
    expect(req.request.responseType).toBe('blob');
    const blob = new Blob(['fake-image']);
    req.flush(blob);

    expect(result).toBe(blob);
  });
});
