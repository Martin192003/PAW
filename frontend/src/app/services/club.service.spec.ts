import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ClubService } from './club.service';
import { Event } from '../models/event.model';
import { Club } from '../models/club.model';
import { AvailabilityPagedResponse } from '../models/availability-paged-response.model';

describe('ClubService event photo links', () => {
  let service: ClubService;
  let httpMock: HttpTestingController;

  const clubId = 'club-1';
  const eventId = 'event-1';

  const eventWithPhotoLink = {
    eventId,
    eventName: 'Fiesta',
    description: 'desc',
    date: '2026-08-01',
    links: [{ rel: 'photo', href: '/api/clubs/club-1/events/event-1/photo' }],
  } as unknown as Event;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ClubService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ClubService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // Bug real de esta sesion: uploadEventPhoto/deleteEventPhoto no normalizaban
  // el campo "links" (array HATEOAS [{rel,href}]) a un Record<string,string>
  // como si lo hacen createEvent/updateEvent. Como consecuencia,
  // getEventPhotoUrl() (que lee event.links?.['photo']) siempre daba
  // undefined justo despues de subir la foto en el alta de un evento.
  it('normalizes the links array returned by uploadEventPhoto into a Record', () => {
    const file = new File(['fake-image'], 'foto.png', { type: 'image/png' });

    let result: Event | undefined;
    service.uploadEventPhoto(clubId, eventId, file, eventWithPhotoLink).subscribe(event => {
      result = event;
    });

    const req = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}/photo`));
    req.flush({
      eventId,
      eventName: 'Fiesta',
      description: 'desc',
      date: '2026-08-01',
      photoId: 'photo-1',
      links: [
        { rel: 'self', href: `/api/clubs/${clubId}/events/${eventId}` },
        { rel: 'photo', href: `/api/clubs/${clubId}/events/${eventId}/photo` },
      ],
    });

    expect(result).toBeTruthy();
    expect(Array.isArray(result?.links)).toBe(false);
    expect((result?.links as unknown as Record<string, string>)?.['photo']).toBe(`/api/clubs/${clubId}/events/${eventId}/photo`);
  });

  it('normalizes the links array returned by deleteEventPhoto into a Record', () => {
    let result: Event | undefined;
    service.deleteEventPhoto(clubId, eventId, eventWithPhotoLink).subscribe(event => {
      result = event;
    });

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}/photo`));
    req.flush({
      eventId,
      eventName: 'Fiesta',
      description: 'desc',
      date: '2026-08-01',
      links: [
        { rel: 'self', href: `/api/clubs/${clubId}/events/${eventId}` },
      ],
    });

    expect(result).toBeTruthy();
    expect(Array.isArray(result?.links)).toBe(false);
    expect((result?.links as unknown as Record<string, string>)?.['self']).toBe(`/api/clubs/${clubId}/events/${eventId}`);
  });

  it('falls back to a constructed URL when no event links are given', () => {
    const file = new File(['fake-image'], 'foto.png', { type: 'image/png' });

    service.uploadEventPhoto(clubId, eventId, file, undefined).subscribe();

    const req = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}/photo`));
    req.flush({ eventId, eventName: 'Fiesta', description: 'desc', date: '2026-08-01', links: [] });
  });

  it('errors without making a request when the event has links but none for "photo"', () => {
    const file = new File(['fake-image'], 'foto.png', { type: 'image/png' });
    const eventWithoutPhotoLink = {
      ...eventWithPhotoLink,
      links: [{ rel: 'self', href: `/api/clubs/${clubId}/events/${eventId}` }],
    } as unknown as Event;

    let error: unknown;
    service.uploadEventPhoto(clubId, eventId, file, eventWithoutPhotoLink).subscribe({
      error: err => { error = err; },
    });

    expect(error).toBeInstanceOf(Error);
    httpMock.expectNone(request => request.url.includes('/photo'));
  });

  it('hydrates event collections returned as HATEOAS item links', () => {
    let result: Event[] | undefined;
    service.getEvents(clubId).subscribe(events => {
      result = events;
    });

    const listReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/events`));
    listReq.flush([
      { rel: 'item', href: `/api/clubs/${clubId}/events/${eventId}` },
    ]);

    const detailReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/events/${eventId}`));
    detailReq.flush({
      id: eventId,
      eventName: 'Fiesta',
      description: 'desc',
      date: '2026-08-01',
      photoId: 'photo-1',
      links: [
        { rel: 'self', href: `/api/clubs/${clubId}/events/${eventId}` },
        { rel: 'photo', href: `/api/clubs/${clubId}/events/${eventId}/photo` },
        { rel: 'update', href: `/api/clubs/${clubId}/events/${eventId}` },
        { rel: 'delete', href: `/api/clubs/${clubId}/events/${eventId}` },
      ],
    });

    expect(result).toHaveLength(1);
    expect(result?.[0].eventId).toBe(eventId);
    expect(result?.[0].eventName).toBe('Fiesta');
    expect(result?.[0].date).toBe('2026-08-01');
    expect((result?.[0].links as unknown as Record<string, string>)?.['photo']).toBe(`/api/clubs/${clubId}/events/${eventId}/photo`);
  });
});

// getClubs/getClubsByUser (vía clubPageFromResponse) son el método más usado del
// servicio y, pese a ser el que motivó todo el rediseño de paginación auditado en
// AUDITORIA_ENTREGA_FINAL.md (puntos 11 y 20), no tenían ningún test propio: la spec
// de arriba sólo cubre uploadEventPhoto/deleteEventPhoto (2 de ~25 métodos públicos).
describe('ClubService getClubs', () => {
  let service: ClubService;
  let httpMock: HttpTestingController;

  const club = (id: string): Club => ({
    clubId: id,
    clubName: `Club ${id}`,
    description: 'desc',
    location: 'loc',
    zone: 'ZONA_CABA',
    musicGenre: 'TECHNO',
    priceRange: 'MEDIO',
    bar: false,
    rating: 4,
    quantityRatings: 10,
    createdAt: '2026-01-01T00:00:00Z',
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ClubService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ClubService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('resolves a link-only collection with N+1 requests and reads total/availability from headers', () => {
    let result: AvailabilityPagedResponse<Club> | undefined;
    service.getClubs(1, 10).subscribe(page => { result = page; });

    const listReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/clubs'));
    listReq.flush(
      [{ rel: 'item', href: '/api/clubs/club-1' }, { rel: 'item', href: '/api/clubs/club-2' }],
      { headers: { 'X-Total-Count': '2', 'X-Available-Today': 'false' } }
    );

    const club1Req = httpMock.expectOne(request => request.url.endsWith('/clubs/club-1'));
    club1Req.flush(club('club-1'));
    const club2Req = httpMock.expectOne(request => request.url.endsWith('/clubs/club-2'));
    club2Req.flush(club('club-2'));

    expect(result?.totalElements).toBe(2);
    expect(result?.content.map(c => c.clubId).sort()).toEqual(['club-1', 'club-2']);
  });

  it('falls back to an embedded array without extra requests for a legacy (non-link) response', () => {
    let result: AvailabilityPagedResponse<Club> | undefined;
    service.getClubs(1, 10).subscribe(page => { result = page; });

    const listReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/clubs'));
    listReq.flush([club('club-1')], { headers: { 'X-Total-Count': '1' } });

    httpMock.expectNone(request => request.url.endsWith('/clubs/club-1'));
    expect(result?.content[0].clubId).toBe('club-1');
  });

  it('getClubsByUser sends ownerId as a query param', () => {
    service.getClubsByUser('user-1', 1, 10).subscribe();

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/clubs'));
    expect(req.request.params.get('ownerId')).toBe('user-1');
    req.flush([], { headers: { 'X-Total-Count': '0' } });
  });
});
