import { PagedResponse } from './paged-response.model';

export interface AvailabilityPagedResponse<T> extends PagedResponse<T> {
  availableToday: boolean;
  invalidDay: boolean;
  selectedAvailabilityDate?: string | null;
}
