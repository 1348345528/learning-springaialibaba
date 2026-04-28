import { Navigate, useLocation } from 'react-router-dom';
import useAuthStore from '../store/useAuthStore';

const RequireAuth = ({ children }) => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};

export default RequireAuth;
