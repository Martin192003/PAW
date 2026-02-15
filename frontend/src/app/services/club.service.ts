import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Club } from '../models/club.model';
import { ClubLimits } from '../models/club-limits.model';
import { Cover } from '../models/cover.model';
import { Layout } from '../models/layout.model';

@Injectable({
  providedIn: 'root'
})
export class ClubService {

  private readonly apiUrl = 'http://localhost:8080/webapp_war_exploded/api';

  constructor(private http: HttpClient) {}

  getClubs(): Observable<Club[]> {
    return this.http.get<Club[]>(`${this.apiUrl}/clubs`);
  }

  getClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`);
  }

  getCover(id: string): Observable<Cover> {
    return this.http.get<Cover>(`${this.apiUrl}/clubs/${id}/cover`);
  }

  getLayout(id: string): Observable<Layout> {
    return this.http.get<Layout>(`${this.apiUrl}/clubs/${id}/layout`);
  }

  getLimits(id: string): Observable<ClubLimits> {
    return this.http.get<ClubLimits>(`${this.apiUrl}/clubs/${id}/limits`);
  }
}
