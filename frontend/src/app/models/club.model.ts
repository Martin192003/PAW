export interface Link {
  rel: string;
  href: string;
  method?: string;
}

export type LinkCollection = Record<string, string> | Link[] | { entry?: Link[] };

export interface ClubLinks {
  [rel: string]: string | undefined;
  self?: string;
  cover?: string;
  layout?: string;
  limits?: string;
  events?: string;
  bookings?: string;
  reviews?: string;
  photos?: string;
  menu?: string;
  tables?: string;
  owner?: string;
}

export interface Club {
  clubId: string;
  clubName: string;
  description: string;
  location: string;
  zone: string;
  musicGenre: string;
  priceRange: string;
  bar: boolean;
  cbu: string;
  rating: number;
  quantityRatings: number;
  createdAt: string;

  links?: ClubLinks;
}

export interface ClubCreateRequest {
  clubName: string;
  location: string;
  priceRange: string;
  description: string;
  musicGenre: string;
  zone: string;
  bar: boolean;
  cbu: string;
}
