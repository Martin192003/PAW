export interface Booking {
  id: string;
  clubId: string;
  clubName?: string;
  userId: string;
  userUsername?: string;
  userName?: string;
  userSurname?: string;
  tableId: string;
  tableNumber?: number;
  tablePrice?: number;
  tableDiscount?: number;
  bookingStart: string;
  bookingEnd: string;
  status: 'PENDIENTE' | 'CONFIRMADA' | 'CANCELADA' | string;
  createdAt: string;
  totalAmount: number;
  hasReceipt?: boolean;
  links?: Record<string, string> | Array<{ rel: string; href: string }>;
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
  links?: Record<string, string> | Array<{ rel: string; href: string }>;
}
