import { TestBed } from '@angular/core/testing';
import { ApiErrorService } from './api-error.service';

// api-error.service.ts no tenia ningun test (ver AUDITORIA_ENTREGA_FINAL.md punto 25).
describe('ApiErrorService', () => {
  let service: ApiErrorService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ApiErrorService] });
    service = TestBed.inject(ApiErrorService);
  });

  it('starts with no error', () => {
    expect(service.getSnapshot()).toBeNull();
  });

  it('setError updates the snapshot and notifies subscribers', () => {
    const seen: (ReturnType<ApiErrorService['getSnapshot']>)[] = [];
    service.error$.subscribe(value => seen.push(value));

    service.setError({ status: 404, message: 'not_found' });

    expect(service.getSnapshot()).toEqual({ status: 404, message: 'not_found' });
    expect(seen).toEqual([null, { status: 404, message: 'not_found' }]);
  });

  it('clear resets the error back to null', () => {
    service.setError({ status: 500, message: 'server_error' });
    service.clear();

    expect(service.getSnapshot()).toBeNull();
  });
});
