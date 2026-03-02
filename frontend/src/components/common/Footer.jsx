import { Link } from 'react-router-dom';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-white dark:bg-gray-800 border-t border-gray-200 dark:border-gray-700 px-6 py-4">
      <div className="flex flex-col md:flex-row items-center justify-between text-sm text-gray-600 dark:text-gray-400">
        <div className="mb-2 md:mb-0">
          &copy; {currentYear} Disaster Resilience Hub. All rights reserved.
        </div>
        <div className="flex space-x-4">
          <Link to="/about" className="hover:text-primary-600 dark:hover:text-primary-400">
            About
          </Link>
          <Link to="/privacy" className="hover:text-primary-600 dark:hover:text-primary-400">
            Privacy
          </Link>
          <Link to="/terms" className="hover:text-primary-600 dark:hover:text-primary-400">
            Terms
          </Link>
          <Link to="/contact" className="hover:text-primary-600 dark:hover:text-primary-400">
            Contact
          </Link>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
