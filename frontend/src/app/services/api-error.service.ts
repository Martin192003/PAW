import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ApiErrorState = {
  status: number;
  message: string;
} | null;

@Injectable({ providedIn: 'root' })
export class ApiErrorService {
  private readonly errorSubject = new BehaviorSubject<ApiErrorState>(null);

  get error$(): Observable<ApiErrorState> {
    return this.errorSubject.asObservable();
  }

  getSnapshot(): ApiErrorState {
    return this.errorSubject.value;
  }

  setError(error: ApiErrorState): void {
    this.errorSubject.next(error);
  }

  clear(): void {
    this.errorSubject.next(null);
  }
}
