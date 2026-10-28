// Maps the token type names the backend reports (ANTLR symbolic names of the three lexers) to syntax categories.
// The category names are CSS classes in styles/code.css.
const TYPE_NAMES = new Set([
  'NUMERUS', 'TEXTUM', 'DECIMALIS', 'LITTERA', 'BOOL', 'SERIES',
  'ENTERO', 'CADENA', 'FLOTANTE', 'CARACTER',
  'INT', 'DOUBLE', 'CHAR', 'BOOLEAN', 'STRING', 'VOID',
])

const LITERAL_NAMES = new Set(['VERUM', 'FALSUS', 'NULL', 'TRUE', 'FALSE', 'VERDADERO', 'FALSO'])

const OPERATOR_NAMES = new Set([
  'PLUS', 'MINUS', 'STAR', 'DIV', 'MOD', 'EQ', 'NEQ', 'LT', 'GT', 'LE', 'GE', 'AND', 'OR', 'NOT',
  'ASSIGN', 'ARROW', 'INC', 'DEC', 'PLUS_PLUS', 'MINUS_MINUS', 'PLUS_ASSIGN', 'MINUS_ASSIGN', 'MUL_ASSIGN',
  'PRINT_OP', 'READ_OP', 'QUESTION',
])

// FUNCTIONS_SECTION (Pig "MUNERA>"; the body after it produces no tokens) is plain on purpose: validation rejects that
// section, and a painted keyword would hide the red underline of the error.
const PLAIN_NAMES = new Set([
  'FUNCTIONS_SECTION',
  'LBRACE', 'RBRACE', 'LPAREN', 'RPAREN', 'LBRACK', 'RBRACK', 'LBRACKET', 'RBRACKET',
  'SEMI', 'COMMA', 'DOT', 'COLON', 'WS', 'NEWLINE', 'INDENT', 'DEDENT', 'EOF',
])

// Visibility modifiers and inheritance: the keyword family, in a lighter violet so they read apart from control flow.
const MODIFIER_NAMES = new Set(['PUBLIC', 'PRIVATE', 'PROTECTED', 'EXTENDS'])

const ANNOTATION_NAMES = new Set(['OVERRIDE'])

// Reserved words listed explicitly: a token name the backend adds later is not in any set and falls back to plain
// text instead of being guessed from its spelling.
const KEYWORD_NAMES = new Set([
  // Pig Latin
  'IMPORT', 'ESTO', 'NOVUS', 'SI', 'ALITER', 'FINIS', 'DUM', 'FACERE', 'PER', 'PERGE', 'INTERRUMPE', 'NON',
  'VARIABILES_SECTION', 'MAIOR_SECTION', 'PROGRAM_END',
  // Y?
  'ESTRUCTURA', 'DEFINIR', 'RETORNAR', 'ENTONCES', 'SINO', 'CONTRARIO', 'ELEGIR', 'CASO', 'SIEMPRE', 'ROMPER',
  'PARA', 'MIENTRAS', 'HACER', 'CONTINUAR', 'IMPRIMIR', 'LEER', 'SECCION_ESTRUCTURAS', 'SECCION_FUNCIONES',
  // Zetariano
  'CLASS', 'NEW', 'RETURN', 'IF', 'ELSE', 'WHILE', 'DO', 'FOR', 'SWITCH', 'CASE', 'DEFAULT', 'BREAK', 'CONTINUE',
  'THIS', 'SUPER', 'PRINTLN', 'PRINT', 'READLN',
])

const DECLARES_TYPE_NAMES = new Set(['NEW', 'NOVUS', 'CLASS', 'EXTENDS'])

function classifyByName(type) {
  if (!type) return 'plain'
  if (PLAIN_NAMES.has(type)) return 'plain'
  if (type === 'STRING_LITERAL' || type === 'CHAR_LITERAL') return 'str'
  if (type.endsWith('_LITERAL')) return 'num'
  if (LITERAL_NAMES.has(type)) return 'num'
  if (TYPE_NAMES.has(type)) return 'ty'
  if (OPERATOR_NAMES.has(type)) return 'op'
  if (type === 'IDENTIFIER' || type === 'ID') return 'id'
  if (MODIFIER_NAMES.has(type)) return 'mod'
  if (ANNOTATION_NAMES.has(type)) return 'an'
  return KEYWORD_NAMES.has(type) ? 'kw' : 'plain'
}

// Comments arrive as hidden tokens. Identifiers are refined by their neighbours: a name followed by "(" is a
// function or method, a name after new/novus/class or followed by another name is a type.
export function classifyTokens(tokens) {
  const visible = tokens.filter((token) => !token.hidden)
  const refined = new Map()

  visible.forEach((token, index) => {
    if (classifyByName(token.type) !== 'id') return
    const previous = visible[index - 1]?.type
    const next = visible[index + 1]?.type
    if (DECLARES_TYPE_NAMES.has(previous) || next === 'IDENTIFIER') refined.set(token, 'ty')
    else if (next === 'LPAREN') refined.set(token, 'fn')
    else refined.set(token, 'plain')
  })

  return tokens.map((token) => {
    if (token.hidden) return { ...token, cat: 'cm' }
    const cat = refined.get(token) ?? classifyByName(token.type)
    return { ...token, cat: cat === 'id' ? 'plain' : cat }
  })
}
