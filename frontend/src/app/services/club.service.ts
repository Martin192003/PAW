import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, map, throwError } from 'rxjs';
import { Club, ClubCreateRequestDto } from '../models/club.model';
import { environment } from '../../environments/environment';
import { resolveApiUrl, normalizeLinks, requireAnyLink } from '../utils/api-url.util';
import { ClubLimits } from '../models/club-limits.model';
import { Layout } from '../models/layout.model';
import {AvailabilityPagedResponse} from '../models/availability-paged-response.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Review } from '../models/review.model';
import { Event } from '../models/event.model';
import { ClubPhoto } from '../models/club-photo.model';
import { PagedResponse } from '../models/paged-response.model';

export interface ClubUpdatePayload {
  clubName: string;
  location: string;
  priceRange: string;
  description: string;
  musicGenre: string;
  zone: string;
  isBar: boolean;
  cbu: string;
}

export interface ProductPayload {
  productName: string;
  description: string;
  basePrice: number;
  productType: string;
  isAvailable: boolean;
}

export interface TablePayload {
  tableNumber: number;
  capacity: number;
  price: number;
  discount: number;
  tableState: string;
}

export interface EventPayload {
  eventName: string;
  description: string;
  date: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClubService {

  private readonly apiUrl = environment.apiBaseUrl;

  private readonly emptyFilter = {
    name: '',
    zone: '',
    genre: '',
    price: '',
    isBar: '',
    sort: ''
  };

  private readonly linkRels = {
    photos: ['photos', 'club-photos', 'gallery'],
    reviewPhotos: ['review-photos', 'reviewPhotos', 'review-photos'],
    reviews: ['reviews', 'club-reviews'],
    events: ['events', 'club-events'],
    menu: ['menu', 'club-menu'],
    tables: ['tables', 'club-tables'],
    bookings: ['bookings', 'club-bookings'],
    layout: ['layout', 'club-layout'],
  };

  constructor(private readonly http: HttpClient) {}

  private buildNoCacheParams(seed?: HttpParams): HttpParams {
    const params = seed ?? new HttpParams();
    return params.set('_ts', Date.now().toString());
  }

  private normalizeClub(club: Club): Club {
    return {
      ...club,
      links: normalizeLinks(club.links)
    };
  }

  private normalizeClubs(clubs: Club[]): Club[] {
    return clubs.map(club => this.normalizeClub(club));
  }

  private resolveClubLink(links: unknown, rels: string[], context: string): string {
    return resolveApiUrl(requireAnyLink(links as any, rels, context), this.apiUrl);
  }

  private missingLinkError(err: unknown): Observable<never> {
    return throwError(() => err instanceof Error ? err : new Error('Missing HATEOAS link.'));
  }

  getClubs(
    page: number,
    size: number,
    filters: Partial<{ name: string; zone: string; genre: string; price: string; isBar: string; sort: string }> = this.emptyFilter
  ): Observable<AvailabilityPagedResponse<Club>> {
    const normalized = { ...this.emptyFilter, ...filters };

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (normalized.name) params = params.set('name', normalized.name);
    if (normalized.zone) params = params.set('zone', normalized.zone);
    if (normalized.genre) params = params.set('genre', normalized.genre);
    if (normalized.price) params = params.set('price', normalized.price);
    if (normalized.isBar) params = params.set('isBar', normalized.isBar);
    if (normalized.sort) params = params.set('sort', normalized.sort);

    return this.http.get<AvailabilityPagedResponse<Club>>(`${this.apiUrl}/clubs`, { params }).pipe(
      map(response => ({
        ...response,
        content: this.normalizeClubs(response.content ?? [])
      }))
    );
  }

  getClubsByUser(userId: string, page: number, size: number): Observable<PagedResponse<Club>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<Club>>(
      `${this.apiUrl}/clubs/user/${encodeURIComponent(userId)}`,
      { params }
    ).pipe(
      map(response => ({
        ...response,
        content: this.normalizeClubs(response.content ?? [])
      }))
    );
  }

  createClub(payload: ClubCreateRequestDto): Observable<Club> {
    return this.http.post<Club>(`${this.apiUrl}/clubs`, payload).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  getClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`, {
      params: this.buildNoCacheParams()
    }).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  getClubByLink(link: string): Observable<Club> {
    return this.http.get<Club>(resolveApiUrl(link, this.apiUrl)).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  getCoverUrl(id: string): string {
    return resolveApiUrl(`${this.apiUrl}/clubs/${id}/cover`, this.apiUrl);
  }

  getPhotoUrl(photoId: string): string {
    return resolveApiUrl(`${this.apiUrl}/photos/${photoId}`, this.apiUrl);
  }

  getClubPhotos(id: string, links?: unknown): Observable<ClubPhoto[]> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.photos, `club ${id} photos`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http
      .get<ClubPhoto[] | { photos?: ClubPhoto[] }>(url)
      .pipe(
        map(response => Array.isArray(response) ? response : (response?.photos ?? []))
      );
  }

  getReviewPhotosByClub(id: string, links?: unknown): Observable<ClubPhoto[]> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviewPhotos, `club ${id} review photos`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http
      .get<ClubPhoto[] | { photos?: ClubPhoto[] }>(url)
      .pipe(
        map(response => Array.isArray(response) ? response : (response?.photos ?? [])),
        map(photos => photos
          .map(photo => ({
            ...photo,
            url: this.toAbsoluteUrl(photo.url, id)
          }))
          .filter(photo => !!photo.url)
        )
      );
  }

  getLayout(id: string, links?: unknown): Observable<Layout> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.layout, `club ${id} layout`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Layout>(url);
  }

  uploadCover(clubId: string, file: File): Observable<unknown> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.put(`${this.apiUrl}/clubs/${clubId}/cover`, formData);
  }

  uploadLayout(clubId: string, file: File): Observable<unknown> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.put(`${this.apiUrl}/clubs/${clubId}/layout`, formData);
  }

  uploadGalleryPhotos(clubId: string, files: File[]): Observable<unknown> {
    const uploads = files.map((file) => {
      const formData = new FormData();
      formData.append('file', file);
      return this.http.post(`${this.apiUrl}/clubs/${clubId}/photos`, formData);
    });

    return forkJoin(uploads);
  }

  deleteGalleryPhoto(clubId: string, photoId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clubs/${clubId}/photos/${photoId}`);
  }

  toAbsoluteUrl(path: string, clubId?: string): string {
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    if (!path && clubId) {
      return resolveApiUrl(`${this.apiUrl}/clubs/${clubId}/layout/image`, this.apiUrl);
    }
    const normalizedPath = path.startsWith('/') ? path.slice(1) : path;
    return resolveApiUrl(`/${normalizedPath}`, this.apiUrl);
  }

  getLimits(id: string): Observable<ClubLimits> {
    return this.http.get<ClubLimits>(`${this.apiUrl}/clubs/${id}/limits`);
  }
  getMenu(id: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.menu, `club ${id} menu`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Product[]>(url, {
      params: this.buildNoCacheParams()
    });
  }

  getTables(id: string, date?: string, links?: unknown) {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }

    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.tables, `club ${id} tables`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<ClubTable[]>(url, {
      params: this.buildNoCacheParams(params)
    });
  }

  reserveTable(clubId: string, tableId: string, date: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.bookings, `club ${clubId} bookings`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.post<{
      id: string;
      tableId: string;
      bookingStart: string;
      bookingEnd: string;
      status: string;
      links: Record<string, string>;
    }>(
      url,
      { tableId, date }
    );
  }

  getReviews(id: string, page = 0, size = 10, links?: unknown): Observable<Review[]> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviews, `club ${id} reviews`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(url, { params }).pipe(
      map(response => Array.isArray(response) ? response : (response?.content ?? []))
    );
  }

  getReviewsPaginated(id: string, page = 0, size = 10, links?: unknown): Observable<any> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviews, `club ${id} reviews`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(url, { params });
  }

  getEvents(id: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.events, `club ${id} events`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Array<Event & { id?: string }>>(url, {
      params: this.buildNoCacheParams()
    }).pipe(
      map((events) =>
        (events ?? []).map((event) => ({
          ...event,
          eventId: event.eventId || event.id || '',
          links: normalizeLinks(event.links)
        }))
      )
    );
  }

  updateClub(clubId: string, payload: ClubUpdatePayload): Observable<Club> {
    return this.http.put<Club>(`${this.apiUrl}/clubs/${clubId}`, payload);
  }

  createProduct(clubId: string, payload: ProductPayload): Observable<Product> {
    return this.http.post<Product>(`${this.apiUrl}/clubs/${clubId}/menu`, payload);
  }

  updateProduct(clubId: string, productId: string, payload: ProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/clubs/${clubId}/menu/${productId}`, payload);
  }

  createTable(clubId: string, payload: TablePayload): Observable<ClubTable> {
    return this.http.post<ClubTable>(`${this.apiUrl}/clubs/${clubId}/tables`, payload);
  }

  updateTable(clubId: string, tableId: string, payload: TablePayload): Observable<ClubTable> {
    return this.http.put<ClubTable>(`${this.apiUrl}/clubs/${clubId}/tables/${tableId}`, payload);
  }

  deleteTable(clubId: string, tableId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clubs/${clubId}/tables/${tableId}`);
  }

  createEvent(clubId: string, payload: EventPayload): Observable<Event> {
    return this.http.post<Event>(`${this.apiUrl}/clubs/${clubId}/events`, payload);
  }

  updateEvent(clubId: string, eventId: string, payload: EventPayload): Observable<Event> {
    return this.http.put<Event>(`${this.apiUrl}/clubs/${clubId}/events/${eventId}`, payload);
  }

  deleteEvent(clubId: string, eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clubs/${clubId}/events/${eventId}`);
  }

  uploadEventPhoto(clubId: string, eventId: string, file: File): Observable<Event> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.put<Event>(`${this.apiUrl}/clubs/${clubId}/events/${eventId}/photo`, formData);
  }

  deleteEventPhoto(clubId: string, eventId: string): Observable<Event> {
    return this.http.delete<Event>(`${this.apiUrl}/clubs/${clubId}/events/${eventId}/photo`);
  }
}
