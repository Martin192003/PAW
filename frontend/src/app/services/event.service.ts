import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { ClubEvent, EventPayload } from '../models/ClubEvent.model';
import { PagedResponse } from '../models/paged-response.model';
import { environment } from '../../environments/environment';
import { itemLinksFromCollection, normalizeLinks, requireAnyLink, resolveApiUrl, totalFromHeader } from '../utils/api-url.util';


interface EventDto {
  id: string;
  eventName: string;
  description: string;
  date: string;
  photoId: string | null;
  clubName?: string;
  links?: Record<string, string>;
}

interface RawResponse {
  content?: EventDto[];
  [key: string]: unknown;
}


function toClubEvent(dto: EventDto): ClubEvent {
  const links = normalizeLinks(dto.links);
  const clubLink = links?.['club'];
  const clubId = clubLink ? extractIdFromLink(clubLink, 'clubs') : undefined;
  return {
    eventId:     dto.id,
    name:        dto.eventName,
    description: dto.description,
    date:        dto.date,
    photoId:     dto.photoId ?? '',
    clubName:    dto.clubName ?? '',
    clubId,
    photoUrl:    links?.['photo'],
    links,
  };
}

function extractIdFromLink(link: string, segment: string): string | undefined {
  try {
    const path = link.startsWith('http://') || link.startsWith('https://')
      ? new URL(link).pathname
      : link;
    const parts = path.split('/').filter(Boolean);
    const index = parts.indexOf(segment);
    return index >= 0 ? parts[index + 1] : undefined;
  } catch {
    return undefined;
  }
}

function toPagedResponse(raw: EventDto[] | RawResponse, page = 0, size = 20, total?: number): PagedResponse<ClubEvent> {
  const dtos: EventDto[] = Array.isArray(raw) ? raw : (raw.content ?? []);
  const totalElements = total ?? dtos.length;
  return {
    content:       dtos.map(toClubEvent),
    totalElements,
    totalPages:    Math.ceil(totalElements / size),
    number:        page,
  };
}


@Injectable({ providedIn: 'root' })
export class EventService {

  private readonly base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}


  getUpcomingEvents(page = 0, size = 20): Observable<PagedResponse<ClubEvent>> {
    return this.http
      .get<EventDto[] | RawResponse>(`${this.base}/events`, {
        params: this.pageParams(page, size),
        observe: 'response',
      })
      .pipe(switchMap(response => this.pageFromResponse(response, page, size)));
  }

  getEventsByClub(clubId: string, page = 0, size = 20): Observable<PagedResponse<ClubEvent>> {
    return this.http
      .get<EventDto[] | RawResponse>(`${this.base}/clubs/${clubId}/events`, {
        params: this.pageParams(page, size),
        observe: 'response',
      })
      .pipe(switchMap(response => this.pageFromResponse(response, page, size)));
  }


  getEventById(clubId: string, eventId: string): Observable<ClubEvent> {
    return this.http
      .get<EventDto>(`${this.base}/clubs/${clubId}/events/${eventId}`)
      .pipe(map(toClubEvent));
  }


  createEvent(clubId: string, payload: EventPayload, clubLinks?: Record<string, string>): Observable<ClubEvent> {
    const url = this.resolveEventLink(clubLinks, ['events', 'club-events'], `club ${clubId} events`, `${this.base}/clubs/${clubId}/events`);
    return this.http
      .post<EventDto>(url, payload)
      .pipe(map(toClubEvent));
  }

  updateEvent(clubId: string, eventId: string, payload: EventPayload, event?: ClubEvent): Observable<ClubEvent> {
    const url = this.resolveEventLink(event?.links, ['update', 'self'], `event ${eventId}`, `${this.base}/clubs/${clubId}/events/${eventId}`);
    return this.http
      .put<EventDto>(url, payload)
      .pipe(map(toClubEvent));
  }

  deleteEvent(clubId: string, eventId: string, event?: ClubEvent): Observable<void> {
    const url = this.resolveEventLink(event?.links, ['delete', 'self'], `event ${eventId}`, `${this.base}/clubs/${clubId}/events/${eventId}`);
    return this.http.delete<void>(url);
  }


  getEventPhoto(clubId: string, eventId: string, event?: ClubEvent): Observable<Blob> {
    const url = this.resolveEventLink(event?.links, ['photo'], `event ${eventId} photo`, `${this.base}/clubs/${clubId}/events/${eventId}/photo`);
    return this.http.get(url, {
      responseType: 'blob',
    });
  }

  private resolveEventLink(
    links: Record<string, string> | undefined,
    rels: string[],
    context: string,
    fallback: string
  ): string {
    if (!links) {
      return resolveApiUrl(fallback, this.base);
    }
    return resolveApiUrl(requireAnyLink(links, rels, context), this.base);
  }


  private pageParams(page: number, size: number): HttpParams {
    return new HttpParams().set('page', page).set('size', size);
  }

  private pageFromResponse(
    response: HttpResponse<EventDto[] | RawResponse>,
    page: number,
    size: number
  ): Observable<PagedResponse<ClubEvent>> {
    const body = response.body ?? [];
    const itemLinks = itemLinksFromCollection(body);
    const fallbackTotal = itemLinks?.length ?? (Array.isArray(body) ? body.length : body.content?.length ?? 0);
    const total = totalFromHeader(response.headers.get('X-Total-Count'), fallbackTotal);

    if (itemLinks) {
      const requests = itemLinks.map(link => this.http.get<EventDto>(resolveApiUrl(link, this.base)));
      return (requests.length ? forkJoin(requests) : of([] as EventDto[])).pipe(
        map(events => toPagedResponse(events, page, size, total))
      );
    }

    return of(toPagedResponse(body, page, size, total));
  }
}
