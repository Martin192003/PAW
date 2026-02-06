import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';

@Component({
  selector: 'app-clubs-page',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Listado de Clubs</h2>

    <ul>
      <li *ngFor="let club of clubs">
        {{ club.clubName }} – {{ club.location }}
      </li>
    </ul>
  `
})
export class ClubsPageComponent implements OnInit {

  clubs: Club[] = [];

  constructor(private clubService: ClubService) {}

  ngOnInit(): void {
    this.clubService.getClubs().subscribe({
      next: data => this.clubs = data,
      error: err => console.error(err)
    });
  }
}
