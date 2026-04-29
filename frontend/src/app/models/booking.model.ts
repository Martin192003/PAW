export interface Booking {
  id: string;
  clubId: string;
  userId: string;
  tableId: string;
  bookingStart: string;
  bookingEnd: string;
  status: 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | string;
  createdAt: string;
  totalAmount: number;
  links?: Record<string, string>;
}

export interface BookingPage {
  content: Booking[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BookingFilters {
  clubId?: string;
  status?: string;
  sort?: string;
  bookingDate?: string;
  hasReceipt?: string;
}

export interface Receipt {
  id: string;
  bookingId: string;
  uploadedBy: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  createdAt: string;
  links?: Record<string, string>;
}
