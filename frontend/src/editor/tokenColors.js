// Known operator token names across the supported grammars
const OPERATOR_TYPES = new Set([
  'PLUS',
  'MINUS',
  'STAR',
  'DIV',
  'MOD',
  'POW',
  'EXP',
  'EQ',
  'NEQ',
  'LT',
  'GT',
  'LE',
  'GE',
  'AND',
  'OR',
  'NOT',
  'NON',
  'ASSIGN',
  'ARROW',
  'INC',
  'DEC',
  'PLUS_ASSIGN',
  'MINUS_ASSIGN',
  'MUL_ASSIGN',
  'DIV_ASSIGN',
  'MOD_ASSIGN',
  'PRINT_OP',
  'READ_OP',
  'QUESTION',
  'EXCLAMATION',
  'EQUAL',
  'NOT_EQUAL',
  'LESS',
  'GREATER',
  'LESS_EQUAL',
  'GREATER_EQUAL',
]);

// Delimiter and structural section token names
const DELIMITER_TYPES = new Set([
  'LBRACE',
  'RBRACE',
  'LPAREN',
  'RPAREN',
  'LBRACK',
  'RBRACK',
  'LBRACKET',
  'RBRACKET',
  'SEMI',
  'COMMA',
  'DOT',
  'COLON',
  'COLON_COLON',
  'VARIABILES_SECTION',
  'MAIOR_SECTION',
  'PROGRAM_END',
]);

// Ignored non-visual tokens that should fall back to plain text
const PLAIN_TYPES = new Set(['WS', 'INDENT', 'DEDENT', 'EOF']);

export function classifyToken(tokenType) {
  if (!tokenType || typeof tokenType !== 'string') {
    return 'plain';
  }

  if (PLAIN_TYPES.has(tokenType)) {
    return 'plain';
  }

  if (tokenType.endsWith('STRING_LITERAL') || tokenType === 'STRING_LITERAL') {
    return 'string';
  }

  if (tokenType.endsWith('CHAR_LITERAL') || tokenType === 'CHAR_LITERAL') {
    return 'char';
  }

  if (
    tokenType.endsWith('INT_LITERAL') ||
    tokenType.endsWith('FLOAT_LITERAL') ||
    tokenType.endsWith('DOUBLE_LITERAL') ||
    tokenType.endsWith('NUMBER_LITERAL')
  ) {
    return 'number';
  }

  if (tokenType === 'IDENTIFIER' || tokenType === 'ID') {
    return 'identifier';
  }

  if (OPERATOR_TYPES.has(tokenType)) {
    return 'operator';
  }

  if (DELIMITER_TYPES.has(tokenType)) {
    return 'delimiter';
  }

  // Keywords convention: all-uppercase identifiers that are not literals, operators, or delimiters
  if (/^[A-Z][A-Z0-9_]*$/.test(tokenType)) {
    return 'keyword';
  }

  return 'plain';
}

export const CATEGORY_COLORS = {
  keyword: '#c678dd',     // Purple
  string: '#98c379',      // Green
  char: '#e5c07b',        // Yellow/Gold
  number: '#d19a66',      // Orange
  identifier: '#61afef',  // Light Blue
  operator: '#56b6c2',    // Cyan
  delimiter: '#abb2bf',   // Gray
  comment: '#7f848e',     // Muted Gray
  plain: '#abb2bf',       // Neutral text
};
