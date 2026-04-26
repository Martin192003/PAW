export interface Event {
  eventId: string;
  id?: string;
  eventName: string;
  description: string;
  date: string;
  photoId?: string;
  links?: {
    self?: string;
    club?: string;
    photo?: string;
  };
}
