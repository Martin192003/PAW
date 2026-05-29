export interface Event {
  eventId: string;
  id?: string;
  eventName: string;
  description: string;
  date: string;
  photoId?: string;
  links?: Record<string, string>;
}
