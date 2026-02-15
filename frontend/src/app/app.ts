import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {TopbarComponent} from './components/topbar/topbar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TopbarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = signal('Clubs App');
}
