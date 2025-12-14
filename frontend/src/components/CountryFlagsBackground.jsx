import { useMemo } from 'react';
import { teamToCountryCode } from '../utils/countryFlags';
import './CountryFlagsBackground.css';

const FLAG_CONFIG = {
  COLS: 8,
  ROWS: 6,
  MIN_SIZE: 60,
  MAX_SIZE: 100,
  MIN_OPACITY: 0.3,
  MAX_OPACITY: 0.5,
  RANDOM_OFFSET: 10,
  ROTATION_RANGE: 15,
  DELAY_INCREMENT: 0
};

const CountryFlagsBackground = () => {
  const flags = useMemo(() => {
    // Get all unique country codes from the team mapping
    const countryCodes = Object.values(teamToCountryCode);
    const uniqueCodes = [...new Set(countryCodes)];
    
    // Calculate grid distribution
    const totalFlags = FLAG_CONFIG.COLS * FLAG_CONFIG.ROWS;
    const flagsToShow = uniqueCodes.slice(0, totalFlags);
    
    // Generate flag data with optimized random values
    return flagsToShow.map((code, index) => {
      const col = index % FLAG_CONFIG.COLS;
      const row = Math.floor(index / FLAG_CONFIG.COLS);
      const baseX = (col / FLAG_CONFIG.COLS) * 100;
      const baseY = (row / FLAG_CONFIG.ROWS) * 100;
      
      // Generate random values once per flag
      const random1 = Math.random();
      const random2 = Math.random();
      const random3 = Math.random();
      const random4 = Math.random();
      
      return {
        code,
        id: `${code}-${index}`,
        x: baseX + (random1 - 0.5) * FLAG_CONFIG.RANDOM_OFFSET,
        y: baseY + (random2 - 0.5) * FLAG_CONFIG.RANDOM_OFFSET,
        size: FLAG_CONFIG.MIN_SIZE + random3 * (FLAG_CONFIG.MAX_SIZE - FLAG_CONFIG.MIN_SIZE),
        opacity: FLAG_CONFIG.MIN_OPACITY + random4 * (FLAG_CONFIG.MAX_OPACITY - FLAG_CONFIG.MIN_OPACITY),
        rotation: (random1 - 0.5) * FLAG_CONFIG.ROTATION_RANGE,
        delay: index * FLAG_CONFIG.DELAY_INCREMENT
      };
    });
  }, []);

  const handleImageError = (e) => {
    e.target.style.display = 'none';
  };

  return (
    <div className="country-flags-background" aria-hidden="true">
      {flags.map((flag) => (
        <div
          key={flag.id}
          className="flag-item"
          style={{
            '--delay': `${flag.delay}s`,
            '--x': `${flag.x}%`,
            '--y': `${flag.y}%`,
            '--size': `${flag.size}px`,
            '--opacity': flag.opacity,
            '--rotation': `${flag.rotation}deg`
          }}
        >
          <img
            src={`https://flagcdn.com/80x60/${flag.code}.png`}
            alt=""
            loading="lazy"
            onError={handleImageError}
          />
        </div>
      ))}
    </div>
  );
};

export default CountryFlagsBackground;

