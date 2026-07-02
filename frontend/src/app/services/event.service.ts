import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { ClubEvent, EventPayload } from '../models/ClubEvent.model';
import { PagedResponse } from '../models/paged-response.model';
import { environment } from '../../environments/environment';
import { normalizeLinks, resolveApiUrl } from '../utils/api-url.util';


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
      .get<EventDto[] | RawResponse>(`${this.base}/clubs/events`, { params: this.pageParams(page, size) })
      .pipe(map(toPagedResponse));
  }

  getEventsByClub(clubId: string, page = 0, size = 20): Observable<PagedResponse<ClubEvent>> {
    return this.http
      .get<EventDto[] | RawResponse>(`${this.base}/clubs/${clubId}/events`, { params: this.pageParams(page, size) })
      .pipe(map(toPagedResponse));
  }

  getEventById(clubId: string, eventId: string): Observable<ClubEvent> {
    return this.http
      .get<EventDto>(`${this.base}/clubs/${clubId}/events/${eventId}`)
      .pipe(map(toClubEvent));
  }


  createEvent(clubId: string, payload: EventPayload): Observable<ClubEvent> {
    return this.http
      .post<EventDto>(`${this.base}/clubs/${clubId}/events`, payload)
      .pipe(map(toClubEvent));
  }

  updateEvent(clubId: string, eventId: string, payload: EventPayload): Observable<ClubEvent> {
    return this.http
      .put<EventDto>(`${this.base}/clubs/${clubId}/events/${eventId}`, payload)
      .pipe(map(toClubEvent));
  }

  deleteEvent(clubId: string, eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/clubs/${clubId}/events/${eventId}`);
  }


  getEventPhoto(clubId: string, eventId: string): Observable<Blob> {
    return this.http.get(`${this.base}/clubs/${clubId}/events/${eventId}/photo`, {
      responseType: 'blob',
    });
  }


  private pageParams(page: number, size: number): HttpParams {
    return new HttpParams().set('page', page).set('size', size);
  }
}
