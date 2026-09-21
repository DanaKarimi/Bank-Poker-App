import React from 'react';
import { AlertCircle, RefreshCw, ArrowLeft } from 'lucide-react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[ErrorBoundary caught display error]:', error, errorInfo);
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null });
    if (this.props.onRetry) {
      this.props.onRetry();
    } else {
      window.location.reload();
    }
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-felt-dark flex items-center justify-center p-4">
          <div className="bg-felt-card border border-gold-accent/40 rounded-2xl p-6 max-w-md w-full text-center space-y-4 shadow-xl">
            <AlertCircle className="w-12 h-12 text-lose-red mx-auto" />
            <h2 className="text-lg font-bold text-cream-text">
              {this.props.fallbackTitle || 'Display Error'}
            </h2>
            <p className="text-xs text-cream-text/70">
              {this.state.error?.message || 'An unexpected rendering error occurred.'}
            </p>
            <div className="flex items-center justify-center gap-3 pt-2">
              <button
                type="button"
                onClick={this.handleRetry}
                className="px-4 py-2.5 bg-felt-dark hover:bg-felt-card border border-gold-accent/40 rounded-xl text-gold-accent text-xs font-bold transition flex items-center gap-2 cursor-pointer"
              >
                <RefreshCw className="w-3.5 h-3.5" />
                <span>Retry</span>
              </button>
              <button
                type="button"
                onClick={() => (window.location.href = '/')}
                className="px-5 py-2.5 bg-gold-accent text-black font-extrabold rounded-xl text-xs uppercase tracking-wider hover:brightness-105 cursor-pointer shadow"
              >
                Dashboard
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
