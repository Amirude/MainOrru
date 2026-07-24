export default function Footer() {
  return (
    <footer style={{ background: 'var(--ink)', color: '#c7cee2', padding: '20px 20px', marginTop: 30, fontSize: '0.82rem' }}>
      <div style={{ maxWidth: 900, margin: '0 auto', display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <div style={{ fontFamily: "'Baloo 2',sans-serif", color: 'var(--yellow)', fontWeight: 700, marginBottom: 4 }}>Altenul One</div>
          {/* Replace with your real registered company name */}
          <div>Altenul One Technologies Pvt. Ltd.</div>
        </div>
        <div>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>Contact</div>
          {/* Replace with your real support contact details */}
          <div>support@altenulone.example</div>
          <div>+91 90000 00000</div>
        </div>
        <div>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>Links</div>
          <div><a href="/support" style={{ color: '#c7cee2' }}>Help &amp; support</a></div>
        </div>
      </div>
      <div style={{ textAlign: 'center', marginTop: 14, color: '#8ea0c9', fontSize: '0.74rem' }}>
        © {new Date().getFullYear()} Altenul One. Replace this footer's details with your real company info.
      </div>
    </footer>
  );
}
