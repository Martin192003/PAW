import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClubService } from '../services/club.service';
import { Club } from '../models/club.model';
import { RouterModule } from '@angular/router';

type ClubListItem = Club & { _hasError?: boolean };

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './clubs.page.html',
})
export class ClubsPage implements OnInit {
  clubs: ClubListItem[] = [];

  constructor(private clubService: ClubService) {}

  ngOnInit() {
    this.clubService.getClubs().subscribe(clubs => {
       this.clubs = clubs;
    });
  }
}
