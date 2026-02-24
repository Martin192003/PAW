import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
  currentPage = 1;
  pageSize = 10;
  totalPages = 0;

  constructor(
    private clubService: ClubService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
    this.loadClubs();
  }

  loadClubs() {
    this.clubService.getClubs(this.currentPage, this.pageSize)
      .subscribe({
        next: (response) => {
          this.clubs = response.content.map(club => ({
            ...club,
            _hasError: false
          }));
          this.totalPages = response.totalPages;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error loading clubs:', err);
        }
      });
  }

  onImageError(event: Event, club: ClubListItem) {
    club._hasError = true;
    this.cdr.detectChanges();
  }

  onImageLoad() {
    this.cdr.detectChanges();
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadClubs();
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadClubs();
    }
  }


  goToPage(page: number) {
    this.currentPage = page;
    this.loadClubs();
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }
}
