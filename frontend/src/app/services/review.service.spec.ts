import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReviewService, ReviewListResult } from './review.service';
import { Review } from '../models/review.model';

// review.service.ts no tenia ningun test propio (ver AUDITORIA_ENTREGA_FINAL.md punto
// 25). Mantiene compatibilidad con colecciones "solo-links", aunque produccion
// ahora usa arrays embebidos para evitar N+1 en pantallas de alto trafico.
describe('ReviewService', () => {
  let service: ReviewService;
  let httpMock: HttpTestingController;

  const clubId = 'club-1';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReviewService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listByClub keeps compatibility with a link-only collection and reads the total from the header', () => {
    let result: ReviewListResult | undefined;
    service.listByClub(clubId, 0, 10).subscribe(res => { result = res; });

    const listReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/reviews`));
    listReq.flush(
      [
        { rel: 'item', href: `/api/clubs/${clubId}/reviews/review-1` },
        { rel: 'item', href: `/api/clubs/${clubId}/reviews/review-2` },
      ],
      { headers: { 'X-Total-Count': '2' } }
    );

    const review1Req = httpMock.expectOne(request => request.url.endsWith(`/reviews/review-1`));
    review1Req.flush({ reviewId: 'review-1', rating: 5, createdAt: '2026-01-01' } as Review);

    const review2Req = httpMock.expectOne(request => request.url.endsWith(`/reviews/review-2`));
    review2Req.flush({ reviewId: 'review-2', rating: 3, createdAt: '2026-01-02' } as Review);

    expect(result?.total).toBe(2);
    expect(result?.reviews.map(r => r.reviewId).sort()).toEqual(['review-1', 'review-2']);
  });

  it('listByClub falls back to an embedded array without extra requests when the body is not link-only', () => {
    let result: ReviewListResult | undefined;
    service.listByClub(clubId, 0, 10).subscribe(res => { result = res; });

    const listReq = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith(`/clubs/${clubId}/reviews`));
    listReq.flush(
      [{ reviewId: 'review-1', rating: 5, createdAt: '2026-01-01' } as Review],
      { headers: { 'X-Total-Count': '1' } }
    );

    httpMock.expectNone(request => request.url.includes('/reviews/review-1'));
    expect(result?.total).toBe(1);
    expect(result?.reviews[0].reviewId).toBe('review-1');
  });

  it('listByUser hits /users/{userId}/reviews with pagination params', () => {
    service.listByUser('user-1', 2, 5).subscribe();

    const req = httpMock.expectOne(request => request.method === 'GET' && request.url.endsWith('/users/user-1/reviews'));
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('5');
    req.flush([], { headers: { 'X-Total-Count': '0' } });
  });

  it('create posts plain JSON when there is no photo', () => {
    service.create(clubId, { rating: 4, description: 'Bien' }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith(`/clubs/${clubId}/reviews`));
    expect(req.request.body).toEqual({ rating: 4, description: 'Bien' });
    req.flush({ reviewId: 'review-1', rating: 4, createdAt: '2026-01-01' });
  });

  it('create posts multipart FormData when a photo is provided', () => {
    const photo = new File(['fake'], 'foto.jpg', { type: 'image/jpeg' });

    service.create(clubId, { rating: 5, photo }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'POST' && request.url.endsWith(`/clubs/${clubId}/reviews`));
    expect(req.request.body instanceof FormData).toBe(true);
    expect((req.request.body as FormData).get('rating')).toBe('5');
    expect((req.request.body as FormData).get('file')).toBe(photo);
    req.flush({ reviewId: 'review-1', rating: 5, createdAt: '2026-01-01' });
  });

  it('update PUTs to the specific review resource', () => {
    service.update(clubId, 'review-1', { rating: 2 }).subscribe();

    const req = httpMock.expectOne(request => request.method === 'PUT' && request.url.endsWith(`/clubs/${clubId}/reviews/review-1`));
    expect(req.request.body).toEqual({ rating: 2 });
    req.flush({ reviewId: 'review-1', rating: 2, createdAt: '2026-01-01' });
  });

  it('delete DELETEs the specific review resource', () => {
    service.delete(clubId, 'review-1').subscribe();

    const req = httpMock.expectOne(request => request.method === 'DELETE' && request.url.endsWith(`/clubs/${clubId}/reviews/review-1`));
    req.flush(null);
  });
});
