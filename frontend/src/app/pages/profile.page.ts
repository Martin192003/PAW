import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { User } from '../models/user.model';
import { AuthStateService } from '../services/auth-state.service';

@Component({
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.css'
})
export class ProfilePage implements OnInit {
  user: User | null = null;

  constructor(
    private readonly authState: AuthStateService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.user = this.authState.getUser();
  }

  goBack(): void {
    this.router.navigate(['/clubs']);
  }
}

