/**
 * MBTI-tailored styling and UI preferences
 * Each personality type has customized UI elements and interaction patterns
 */

export const mbtiProfiles = {
  // Analysts
  INTJ: {
    name: 'The Architect',
    theme: {
      primary: 'indigo',
      accent: 'purple',
      layout: 'minimal',
    },
    preferences: {
      dataVisualization: 'detailed',
      dashboardLayout: 'grid',
      complexity: 'high',
      automation: 'maximum',
    },
    features: ['Advanced Analytics', 'Strategic Planning', 'System Optimization'],
  },
  INTP: {
    name: 'The Logician',
    theme: {
      primary: 'blue',
      accent: 'cyan',
      layout: 'customizable',
    },
    preferences: {
      dataVisualization: 'interactive',
      dashboardLayout: 'flexible',
      complexity: 'high',
      automation: 'selective',
    },
    features: ['Data Analysis', 'Pattern Recognition', 'Research Tools'],
  },
  ENTJ: {
    name: 'The Commander',
    theme: {
      primary: 'red',
      accent: 'orange',
      layout: 'executive',
    },
    preferences: {
      dataVisualization: 'summary',
      dashboardLayout: 'hierarchical',
      complexity: 'medium',
      automation: 'high',
    },
    features: ['Team Management', 'Decision Support', 'Strategic Overview'],
  },
  ENTP: {
    name: 'The Debater',
    theme: {
      primary: 'violet',
      accent: 'pink',
      layout: 'dynamic',
    },
    preferences: {
      dataVisualization: 'varied',
      dashboardLayout: 'flexible',
      complexity: 'medium',
      automation: 'moderate',
    },
    features: ['Scenario Planning', 'Brainstorming Tools', 'Innovation Hub'],
  },

  // Diplomats
  INFJ: {
    name: 'The Advocate',
    theme: {
      primary: 'teal',
      accent: 'green',
      layout: 'harmonious',
    },
    preferences: {
      dataVisualization: 'narrative',
      dashboardLayout: 'story-driven',
      complexity: 'medium',
      automation: 'moderate',
    },
    features: ['Community Impact', 'Humanitarian Focus', 'Long-term Vision'],
  },
  INFP: {
    name: 'The Mediator',
    theme: {
      primary: 'emerald',
      accent: 'lime',
      layout: 'peaceful',
    },
    preferences: {
      dataVisualization: 'empathetic',
      dashboardLayout: 'flow',
      complexity: 'low',
      automation: 'minimal',
    },
    features: ['Individual Stories', 'Values Alignment', 'Personal Impact'],
  },
  ENFJ: {
    name: 'The Protagonist',
    theme: {
      primary: 'rose',
      accent: 'pink',
      layout: 'collaborative',
    },
    preferences: {
      dataVisualization: 'people-focused',
      dashboardLayout: 'social',
      complexity: 'medium',
      automation: 'moderate',
    },
    features: ['Team Collaboration', 'Leadership Tools', 'Communication Hub'],
  },
  ENFP: {
    name: 'The Campaigner',
    theme: {
      primary: 'amber',
      accent: 'yellow',
      layout: 'energetic',
    },
    preferences: {
      dataVisualization: 'colorful',
      dashboardLayout: 'creative',
      complexity: 'medium',
      automation: 'low',
    },
    features: ['Creative Solutions', 'Community Building', 'Inspiration Board'],
  },

  // Sentinels
  ISTJ: {
    name: 'The Logistician',
    theme: {
      primary: 'slate',
      accent: 'gray',
      layout: 'structured',
    },
    preferences: {
      dataVisualization: 'precise',
      dashboardLayout: 'organized',
      complexity: 'medium',
      automation: 'high',
    },
    features: ['Detailed Reports', 'Compliance Tracking', 'Process Management'],
  },
  ISFJ: {
    name: 'The Defender',
    theme: {
      primary: 'sky',
      accent: 'blue',
      layout: 'supportive',
    },
    preferences: {
      dataVisualization: 'clear',
      dashboardLayout: 'traditional',
      complexity: 'low',
      automation: 'moderate',
    },
    features: ['Safety Protocols', 'Care Coordination', 'Resource Management'],
  },
  ESTJ: {
    name: 'The Executive',
    theme: {
      primary: 'neutral',
      accent: 'stone',
      layout: 'professional',
    },
    preferences: {
      dataVisualization: 'factual',
      dashboardLayout: 'dashboard',
      complexity: 'medium',
      automation: 'high',
    },
    features: ['Operations Management', 'KPI Tracking', 'Command Center'],
  },
  ESFJ: {
    name: 'The Consul',
    theme: {
      primary: 'cyan',
      accent: 'teal',
      layout: 'friendly',
    },
    preferences: {
      dataVisualization: 'accessible',
      dashboardLayout: 'welcoming',
      complexity: 'low',
      automation: 'moderate',
    },
    features: ['Community Support', 'Social Coordination', 'Helper Dashboard'],
  },

  // Explorers
  ISTP: {
    name: 'The Virtuoso',
    theme: {
      primary: 'zinc',
      accent: 'neutral',
      layout: 'functional',
    },
    preferences: {
      dataVisualization: 'technical',
      dashboardLayout: 'tools',
      complexity: 'high',
      automation: 'selective',
    },
    features: ['Technical Analysis', 'Hands-on Tools', 'Problem Solving'],
  },
  ISFP: {
    name: 'The Adventurer',
    theme: {
      primary: 'fuchsia',
      accent: 'purple',
      layout: 'artistic',
    },
    preferences: {
      dataVisualization: 'visual',
      dashboardLayout: 'aesthetic',
      complexity: 'low',
      automation: 'low',
    },
    features: ['Visual Documentation', 'Creative Expression', 'Personal Touch'],
  },
  ESTP: {
    name: 'The Entrepreneur',
    theme: {
      primary: 'orange',
      accent: 'red',
      layout: 'action-oriented',
    },
    preferences: {
      dataVisualization: 'real-time',
      dashboardLayout: 'action',
      complexity: 'medium',
      automation: 'moderate',
    },
    features: ['Quick Response', 'Field Operations', 'Live Updates'],
  },
  ESFP: {
    name: 'The Entertainer',
    theme: {
      primary: 'yellow',
      accent: 'amber',
      layout: 'vibrant',
    },
    preferences: {
      dataVisualization: 'engaging',
      dashboardLayout: 'interactive',
      complexity: 'low',
      automation: 'low',
    },
    features: ['Social Engagement', 'Interactive Maps', 'Event Coordination'],
  },
};

export const getMBTIStyle = (mbtiType) => {
  return mbtiProfiles[mbtiType] || mbtiProfiles.ISTJ; // Default to ISTJ
};

export const getThemeColors = (mbtiType) => {
  const profile = getMBTIStyle(mbtiType);
  return {
    primary: profile.theme.primary,
    accent: profile.theme.accent,
  };
};

export const getDashboardLayout = (mbtiType) => {
  const profile = getMBTIStyle(mbtiType);
  return profile.preferences.dashboardLayout;
};

export const getComplexityLevel = (mbtiType) => {
  const profile = getMBTIStyle(mbtiType);
  return profile.preferences.complexity;
};

export const getRecommendedFeatures = (mbtiType) => {
  const profile = getMBTIStyle(mbtiType);
  return profile.features;
};

export const getDataVisualizationStyle = (mbtiType) => {
  const profile = getMBTIStyle(mbtiType);
  return profile.preferences.dataVisualization;
};

export const getAutomationLevel = (mbtiType) => {
  const profile = getMBTIStyle(mbtiType);
  return profile.preferences.automation;
};

// Apply MBTI-specific CSS classes
export const getMBTIClassName = (mbtiType, element = 'container') => {
  const profile = getMBTIStyle(mbtiType);
  const baseClasses = {
    container: 'transition-all duration-300',
    card: 'rounded-lg shadow-md',
    button: 'btn transition-all',
    text: 'font-medium',
  };

  const layoutClasses = {
    minimal: 'space-y-2 p-4',
    customizable: 'space-y-4 p-6',
    executive: 'space-y-3 p-5',
    dynamic: 'space-y-4 p-6',
    harmonious: 'space-y-6 p-8',
    peaceful: 'space-y-8 p-10',
    collaborative: 'space-y-4 p-6',
    energetic: 'space-y-3 p-5',
    structured: 'space-y-2 p-4',
    supportive: 'space-y-4 p-6',
    professional: 'space-y-3 p-5',
    friendly: 'space-y-4 p-6',
    functional: 'space-y-2 p-4',
    artistic: 'space-y-6 p-8',
    'action-oriented': 'space-y-2 p-3',
    vibrant: 'space-y-4 p-6',
    interactive: 'space-y-4 p-6',
  };

  return `${baseClasses[element]} ${layoutClasses[profile.theme.layout] || ''}`;
};
