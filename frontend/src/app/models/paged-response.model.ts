export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number?: number;
  page?: number;
  size?: number;
}
