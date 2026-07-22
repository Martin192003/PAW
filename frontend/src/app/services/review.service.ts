import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Review } from '../models/review.model';
import { environment } from '../../environments/environment';
import { itemLinksFromCollection, resolveApiUrl, totalFromHeader } from '../utils/api-url.util';

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


  listByClub(clubId: string, page = 0, size = 10): Observable<ReviewListResult> {
    const params = new HttpParams()
      .set('clubId', clubId)
      .set('page', page)
      .set('size', size);

    return this.http
      .get<Review[]>(`${this.apiUrl}/reviews`, { params, observe: 'response' })
      .pipe(
        switchMap((response: HttpResponse<Review[]>) => this.listResultFromResponse(response))
      );
  }


  listByUser(userId: string, page = 0, size = 10): Observable<ReviewListResult> {
    const params = new HttpParams()
      .set('userId', userId)
      .set('page', page)
      .set('size', size);

    return this.http
      .get<Review[]>(`${this.apiUrl}/reviews`, { params, observe: 'response' })
      .pipe(
        switchMap((response: HttpResponse<Review[]>) => this.listResultFromResponse(response))
      );
  }


  create(clubId: string, payload: ReviewCreateRequest): Observable<Review> {
    return this.http.post<Review>(`${this.apiUrl}/reviews`, this.toRequestBody(payload), {
      params: new HttpParams().set('clubId', clubId)
    });
  }


  update(clubId: string, reviewId: string, payload: ReviewCreateRequest): Observable<Review> {
    return this.http.put<Review>(`${this.apiUrl}/reviews/${reviewId}`, this.toRequestBody(payload));
  }


  delete(clubId: string, reviewId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/reviews/${reviewId}`);
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

  private listResultFromResponse(response: HttpResponse<Review[]>): Observable<ReviewListResult> {
    const body = response.body ?? [];
    const itemLinks = itemLinksFromCollection(body);
    const total = totalFromHeader(response.headers.get('X-Total-Count'), itemLinks?.length ?? body.length);

    if (itemLinks) {
      const requests = itemLinks.map(link => this.http.get<Review>(resolveApiUrl(link, this.apiUrl)));
      return (requests.length ? forkJoin(requests) : of([] as Review[])).pipe(
        map(reviews => ({ reviews, total }))
      );
    }

    return of({ reviews: Array.isArray(body) ? body : [], total });
  }
}
