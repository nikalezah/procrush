const major = Number.parseInt(process.versions.node.split('.')[0] ?? '0', 10)

if (major < 20) {
  console.error('')
  console.error('ProCrush webReact requires Node.js 20 or newer.')
  console.error(`Current version: ${process.versions.node}`)
  console.error('')
  console.error('The error "Unexpected token \'||=\'" means your Node.js is too old for Vite 6.')
  console.error('')
  console.error('Fix (Windows):')
  console.error('  winget install OpenJS.NodeJS.LTS')
  console.error('  # or download LTS from https://nodejs.org/')
  console.error('')
  console.error('Then reopen the terminal and run:')
  console.error('  node --version   # should show v20+ or v22+')
  console.error('  npm run dev')
  console.error('')
  process.exit(1)
}
