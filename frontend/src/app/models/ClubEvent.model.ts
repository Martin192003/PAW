export interface ClubEvent {
  eventId: string,
  name: string,
  description: string,
  date: string,
  photoId: string,
  clubName: string;
  photoUrl?: string;

  links?:  Record<string, string>

}

export interface EventPayload {
  eventName: string;
  description: string;
  date: string;
}
