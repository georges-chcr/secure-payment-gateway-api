import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ── DTOs alignés sur les records Java ────────────────────────────────────────

export interface PaymentRequest {
  /** Vecteur de 30 features (V1..V28 + Time + Amount). */
  features: number[];
}

export interface PaymentResponse {
  status: string;     // "APPROVED" | "REJECTED"
  riskScore: number;  // probabilité de fraude [0, 1]
  message: string;
}

export interface PaymentTransaction {
  id: number;
  features: number[];
  riskScore: number;
  status: string;
  message: string;
  createdAt: string;  // ISO-8601
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  private readonly apiBase = '/api/v1/payments';

  constructor(private http: HttpClient) {}

  /**
   * Récupère les 50 dernières transactions enregistrées.
   * GET /api/v1/payments/history
   */
  getHistory(): Observable<PaymentTransaction[]> {
    return this.http.get<PaymentTransaction[]>(`${this.apiBase}/history`);
  }

  /**
   * Soumet une transaction au moteur de détection de fraude.
   * POST /api/v1/payments/process
   */
  processPayment(request: PaymentRequest): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.apiBase}/process`, request);
  }

  /**
   * Supprime toutes les transactions de l'historique.
   * DELETE /api/v1/payments/history
   */
  clearHistory(): Observable<void> {
    return this.http.delete<void>(`${this.apiBase}/history`);
  }
}
