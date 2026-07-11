import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ClubEvent, EventPayload } from '../models/ClubEvent.model';
import { PagedResponse } from '../models/paged-response.model';
import { environment } from '../../environments/environment';
import { normalizeLinks, requireAnyLink, resolveApiUrl } from '../utils/api-url.util';


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

function toPagedResponse(raw: EventDto[] | RawResponse): PagedResponse<ClubEvent> {
  const dtos: EventDto[] = Array.isArray(raw) ? raw : (raw.content ?? []);
  return {
    content:       dtos.map(toClubEvent),
    totalElements: dtos.length,
    totalPages:    1,
    number:        0,
  };
}


@Injectable({ providedIn: 'root' })
export class EventService {

  private readonly base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}


  getUpcomingEvents(page = 0, size = 20): Observable<PagedResponse<ClubEvent>> {
    return this.http
      .get<EventDto[] | RawResponse>(`${this.base}/events`, { params: this.pageParams(page, size) })
      .pipe(map(toPagedResponse));
  }

  getEventsByClub(clubId: string, page = 0, size = 20): Observable<PagedResponse<ClubEvent>> {
    return this.http
      .get<EventDto[] | RawResponse>(`${this.base}/clubs/${clubId}/events`, { params: this.pageParams(page, size) })
      .pipe(map(toPagedResponse));
  }

  /**
   * Entrypoint por clubId + eventId (deep link). Si ya tenés el `ClubEvent`
   * cargado (por ejemplo, desde `getEventsByClub`), usá sus `links` en vez de
   * llamar de nuevo con ids sueltos.
   */
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

  /**
   * Resuelve la URL de una acción sobre un evento a partir de sus links
   * HATEOAS. La reconstrucción manual (`fallback`) es un último recurso
   * documentado para cuando todavía no tenemos el `ClubEvent` completo.
   */
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
}
