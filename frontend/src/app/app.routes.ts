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
    path: 'login',
    loadComponent: () => import('./pages/login.page').then(m => m.LoginPage)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register.page').then(m => m.RegisterPage)
  },
  {
    path: 'verify-mail',
    loadComponent: () => import('./pages/verify-mail.page').then(m => m.VerifyMailPage)
  },
  {
    path: 'verify-account',
    loadComponent: () => import('./pages/verify-mail.page').then(m => m.VerifyMailPage)
  },
  {
    path: 'clubs/new',
    loadComponent: () => import('./pages/club-new.page').then(m => m.AddClubComponent)
  },
  {
    path: 'clubs/:id',
    loadComponent: () =>
      import('./pages/club-details.page')
        .then(m => m.ClubDetailsPage)
  },
];
