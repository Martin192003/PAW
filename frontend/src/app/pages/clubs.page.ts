import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClubService } from '../services/club.service';
import { PhotoService } from '../services/photo.service';
import { Club } from '../models/club.model';
import { RouterModule } from '@angular/router';

@Component({
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './clubs.page.html',
})
export class ClubsPage implements OnInit {

  clubs: Club[] = [];

  constructor(
    private clubService: ClubService,
    public photoService: PhotoService
  ) {}

  ngOnInit() {
    this.clubService.getClubs().subscribe(clubs => {
      this.clubs = clubs;
    });
  }
}
