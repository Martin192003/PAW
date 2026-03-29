import { Routes } from '@angular/router';
import { ClubsPage } from './pages/clubs.page';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'clubs',
    pathMatch: 'full'
  },
  {
    path: 'clubs',
    component: ClubsPage
  },
  {
    path: 'clubs/:id',
    loadComponent: () =>
      import('./pages/club-details.page')
        .then(m => m.ClubDetailsPage)
  },
];
