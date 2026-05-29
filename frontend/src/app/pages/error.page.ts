import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiErrorService, ApiErrorState } from '../services/api-error.service';

@Component({
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './error.page.html',
})
export class ErrorPage implements OnInit {
  error: ApiErrorState = null;

  constructor(private readonly apiErrors: ApiErrorService) {}

  ngOnInit(): void {
    this.error = this.apiErrors.getSnapshot();
    this.apiErrors.clear();
  }
}
