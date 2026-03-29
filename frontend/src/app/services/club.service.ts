import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Club } from '../models/club.model';
import { ClubLimits } from '../models/club-limits.model';
import { Layout } from '../models/layout.model';
import {PagedResponse} from '../models/paged-response.model';
import { Product } from '../models/product.model';
import { ClubTable } from '../models/club-table.model';
import { Review } from '../models/review.model';
import { Event } from '../models/event.model';

@Injectable({
  providedIn: 'root'
})
export class ClubService {

  private readonly apiUrl = 'http://localhost:8080/webapp_war_exploded/api';

  constructor(private http: HttpClient) {}

  getClubs(page: number, size: number): Observable<PagedResponse<Club>> {
    return this.http.get<PagedResponse<Club>>(
      `${this.apiUrl}/clubs?page=${page}&size=${size}`
    );
  }

  getClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`);
  }

  getCover(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/clubs/${id}/cover`, {
      responseType: 'blob'
    });
  }

  getCoverUrl(id: string): string {
    return `${this.apiUrl}/clubs/${id}/cover`;
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
    return new URL(path, this.apiUrl).toString();
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

  getReviews(id: string, page = 1) {
    return this.http.get<Review[]>(
      `${this.apiUrl}/clubs/${id}/reviews?page=${page}`
    );
  }

  getEvents(id: string) {
    return this.http.get<Event[]>(
      `${this.apiUrl}/clubs/${id}/events`
    );
  }
}
