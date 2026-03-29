import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { PaymentService, PaymentResponse, PaymentTransaction } from './payment.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  jsonInput = '';
  isLoading = false;
  isClearing = false;
  result = signal<PaymentResponse | null>(null);
  history = signal<PaymentTransaction[]>([]);
  errorMessage = signal<string | null>(null);

  constructor(private paymentService: PaymentService) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.paymentService.getHistory().subscribe({
      next: (data) => this.history.set(data),
      error: (err) => console.error('Erreur chargement historique', err)
    });
  }

  analyze(): void {
    this.errorMessage.set(null);
    this.result.set(null);

    let features: number[];
    try {
      const parsed = JSON.parse(this.jsonInput);
      features = Array.isArray(parsed) ? parsed : parsed['features'];
      if (!Array.isArray(features) || features.length !== 30) {
        throw new Error('Le tableau doit contenir exactement 30 valeurs numériques.');
      }
    } catch (e: any) {
      this.errorMessage.set(e.message || 'JSON invalide.');
      return;
    }

    this.isLoading = true;
    this.paymentService.processPayment({ features }).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading = false;
        setTimeout(() => this.loadHistory(), 800);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Erreur lors de l\'analyse.');
        this.isLoading = false;
      }
    });
  }

  clearHistory(): void {
    this.isClearing = true;
    this.paymentService.clearHistory().subscribe({
      next: () => {
        this.history.set([]);
        this.isClearing = false;
      },
      error: (err) => {
        console.error('Erreur lors de la suppression de l\'historique', err);
        this.isClearing = false;
      }
    });
  }
}
