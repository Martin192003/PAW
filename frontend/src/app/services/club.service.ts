import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Club } from '../models/club.model';
import { ClubLimits } from '../models/club-limits.model';
import { Layout } from '../models/layout.model';
import {AvailabilityPagedResponse} from '../models/availability-paged-response.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Review } from '../models/review.model';
import { Event } from '../models/event.model';
import { ClubPhoto } from '../models/club-photo.model';

@Injectable({
  providedIn: 'root'
})
export class ClubService {

  private readonly apiUrl = 'http://localhost:8080/webapp_war_exploded/api';

  private readonly emptyFilter = {
    name: '',
    zone: '',
    genre: '',
    price: '',
    isBar: '',
    sort: ''
  };

  constructor(private http: HttpClient) {}

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

    return this.http.get<AvailabilityPagedResponse<Club>>(`${this.apiUrl}/clubs`, { params });
  }

  getClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`);
  }

  getCoverUrl(id: string): string {
    return `${this.apiUrl}/clubs/${id}/cover`;
  }

  getClubPhotos(id: string): Observable<ClubPhoto[]> {
    return this.http
      .get<ClubPhoto[] | { photos?: ClubPhoto[] }>(`${this.apiUrl}/clubs/${id}/photos`)
      .pipe(
        map(response => Array.isArray(response) ? response : (response?.photos ?? []))
      );
  }

  getReviewPhotosByClub(id: string): Observable<ClubPhoto[]> {
    return this.http
      .get<ClubPhoto[] | { photos?: ClubPhoto[] }>(`${this.apiUrl}/clubs/${id}/review-photos`)
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

  getLayout(id: string): Observable<Layout> {
    return this.http.get<Layout>(`${this.apiUrl}/clubs/${id}/layout`);
  }

  toAbsoluteUrl(path: string, clubId?: string): string {
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    if (!path && clubId) {
      return `${this.apiUrl}/clubs/${clubId}/layout/image`;
    }
    const normalizedPath = path.startsWith('/') ? path.slice(1) : path;
    return new URL(normalizedPath, `${this.apiUrl}/`).toString();
  }

  getLimits(id: string): Observable<ClubLimits> {
    return this.http.get<ClubLimits>(`${this.apiUrl}/clubs/${id}/limits`);
  }
  getMenu(id: string) {
    return this.http.get<Product[]>(`${this.apiUrl}/clubs/${id}/menu`);
  }

  getTables(id: string, date?: string) {
    const query = date ? `?date=${encodeURIComponent(date)}` : '';
    return this.http.get<ClubTable[]>(`${this.apiUrl}/clubs/${id}/tables${query}`);
  }

  reserveTable(clubId: string, tableId: string, date: string, fromEvent: boolean) {
    return this.http.post<{ bookingId: string; message: string }>(
      `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/reserve`,
      { tableId, date, fromEvent },
      { withCredentials: true }
    );
  }

  getReviews(id: string, page = 0, size = 10): Observable<Review[]> {
    return this.http.get<any>(
      `${this.apiUrl}/clubs/${id}/reviews?page=${page}&size=${size}`
    ).pipe(
      map(response => Array.isArray(response) ? response : (response?.content ?? []))
    );
  }

  getReviewsPaginated(id: string, page = 0, size = 10): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/clubs/${id}/reviews?page=${page}&size=${size}`
    );
  }

  getEvents(id: string) {
    return this.http.get<Event[]>(
      `${this.apiUrl}/clubs/${id}/events`
    );
  }
}
