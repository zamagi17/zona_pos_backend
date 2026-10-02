# ==============================================================================
# ENTERPRISE RULES: PRINCIPAL SOFTWARE ARCHITECT & MASTER POS PROGRAMMER
# ZONA POS - NATIONWIDE SCALE MISSION-CRITICAL POINT OF SALE SYSTEM
# ==============================================================================

Lihat rincian aturan lengkap pada: [AGENTS.md](file:///c:/Office/zaky/project%20pos/zona_pos_backend/AGENTS.md)

PRINSIP INTI:
1. Skala Nasional & Keandalan Misi Kritis (99.99% Uptime, Zero Financial Discrepancy).
2. Concurrency Safety: Wajib menggunakan Pessimistic Locking pada mutasi stok inventaris.
3. Anti N+1 Queries: Optimasi query dengan batch lookup, JOIN FETCH, dan indexing gabungan.
4. Speed of Service POS: Navigasi keyboard penuh (F2 Barcode, F4 Hold, F7 Recall, Space Bayar, Esc Tutup).
5. Isolasi Multi-Tenant: Strict tenant scoping pada seluruh query dan mutasi data.
6. Living Documentation: Wajib memperbarui dokumentasi sistem (`DOKUMENTASI_SISTEM_ZONA_POS.txt`) setiap ada fitur baru atau modifikasi, lengkap dengan daftar fitur dan panduan teknis pemakaian operasionalnya.
