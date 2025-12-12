// Mapping of team names to ISO country codes for flag display
export const teamToCountryCode = {
  // Group A
  'Qatar': 'qa',
  'Ecuador': 'ec',
  'Senegal': 'sn',
  'Netherlands': 'nl',
  
  // Group B
  'England': 'gb-eng',
  'Iran': 'ir',
  'USA': 'us',
  'Wales': 'gb-wls',
  
  // Group C
  'Argentina': 'ar',
  'Saudi Arabia': 'sa',
  'Mexico': 'mx',
  'Poland': 'pl',
  
  // Group D
  'France': 'fr',
  'Australia': 'au',
  'Denmark': 'dk',
  'Tunisia': 'tn',
  
  // Group E
  'Spain': 'es',
  'Costa Rica': 'cr',
  'Germany': 'de',
  'Japan': 'jp',
  
  // Group F
  'Belgium': 'be',
  'Canada': 'ca',
  'Morocco': 'ma',
  'Croatia': 'hr',
  
  // Group G
  'Brazil': 'br',
  'Serbia': 'rs',
  'Switzerland': 'ch',
  'Cameroon': 'cm',
  
  // Group H
  'Portugal': 'pt',
  'Ghana': 'gh',
  'Uruguay': 'uy',
  'South Korea': 'kr',
  
  // Placeholders for knockout stages
  'Group A Winner': 'qa',
  'Group B Runner-up': 'us',
  'Group C Winner': 'ar',
  'Group D Runner-up': 'au',
  'Group E Winner': 'es',
  'Group F Runner-up': 'ca',
  'Group G Winner': 'br',
  'Group H Runner-up': 'gh',
  'QF1 Winner': 'br',
  'QF2 Winner': 'ar',
  'QF3 Winner': 'es',
  'QF4 Winner': 'fr',
  'SF1 Winner': 'br',
  'SF2 Winner': 'ar',
  'Finalist 1': 'br',
  'Finalist 2': 'ar',
};

export const getCountryCode = (teamName) => {
  return teamToCountryCode[teamName] || 'un'; // 'un' for United Nations as fallback
};

export const getFlagUrl = (teamName, size = '80') => {
  const countryCode = getCountryCode(teamName);
  return `https://flagcdn.com/${size}x60/${countryCode}.png`;
};


