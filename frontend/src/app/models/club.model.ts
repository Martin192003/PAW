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

  links?: Record<string, string>;
}

export interface ClubCreateRequestDto {
  clubName: string;
  location: string;
  priceRange: string;
  description: string;
  musicGenre: string;
  zone: string;
  bar: boolean;
  cbu: string;
}
