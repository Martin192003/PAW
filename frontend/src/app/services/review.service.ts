import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Review } from '../models/review.model';
import { environment } from '../../environments/environment';

export interface ReviewCreateRequest {
  rating: number;
  description?: string;
  fromTableGuest?: boolean;
  photo?: File;
}

export interface ReviewListResult {
  reviews: Review[];
  total: number;
}

@Injectable({ providedIn: 'root' })
export class ReviewService {

  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  /**
   * GET /clubs/{clubId}/reviews
   * Lista paginada de reviews. El backend manda X-Total-Count en el header.
   */
  listByClub(clubId: string, page = 0, size = 10): Observable<ReviewListResult> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http
      .get<Review[]>(`${this.apiUrl}/clubs/${clubId}/reviews`, { params, observe: 'response' })
      .pipe(
        map((response: HttpResponse<Review[]>) => {
          const reviews = Array.isArray(response.body) ? response.body : [];
          const totalHeader = response.headers.get('X-Total-Count');
          const total = totalHeader ? Number(totalHeader) : reviews.length;
          return { reviews, total: Number.isFinite(total) ? total : reviews.length };
        })
      );
  }

  /**
   * GET /users/{userId}/reviews
   * Lista paginada de las reviews que hizo el usuario.
   */
  listByUser(userId: string, page = 0, size = 10): Observable<ReviewListResult> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http
      .get<Review[]>(`${this.apiUrl}/users/${userId}/reviews`, { params, observe: 'response' })
      .pipe(
        map((response: HttpResponse<Review[]>) => {
          const reviews = Array.isArray(response.body) ? response.body : [];
          const totalHeader = response.headers.get('X-Total-Count');
          const total = totalHeader ? Number(totalHeader) : reviews.length;
          return { reviews, total: Number.isFinite(total) ? total : reviews.length };
        })
      );
  }

  /**
   * POST /clubs/{clubId}/reviews
   * Crea una review nueva. Requiere estar autenticado.
   */
  create(clubId: string, payload: ReviewCreateRequest): Observable<Review> {
    return this.http.post<Review>(`${this.apiUrl}/clubs/${clubId}/reviews`, this.toRequestBody(payload));
  }

  /**
   * PUT /clubs/{clubId}/reviews
   * Actualiza la review del usuario autenticado para ese club.
   */
  update(clubId: string, payload: ReviewCreateRequest): Observable<Review> {
    return this.http.put<Review>(`${this.apiUrl}/clubs/${clubId}/reviews`, this.toRequestBody(payload));
  }

  /**
   * DELETE /clubs/{clubId}/reviews
   * Borra la review del usuario autenticado para ese club.
   */
  delete(clubId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/clubs/${clubId}/reviews`);
  }

  private toRequestBody(payload: ReviewCreateRequest): ReviewCreateRequest | FormData {
    if (!payload.photo) {
      return payload;
    }

    const formData = new FormData();
    formData.append('rating', String(payload.rating));
    if (payload.description) {
      formData.append('description', payload.description);
    }
    if (payload.fromTableGuest !== undefined) {
      formData.append('fromTableGuest', String(payload.fromTableGuest));
    }
    formData.append('file', payload.photo);
    return formData;
  }
}
