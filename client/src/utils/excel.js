import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

/**
 * 将对象数组导出为 Excel(.xlsx) 并触发浏览器下载
 * @param {Array<Object>} rows 数据行
 * @param {Array<{label:string, key:string}>} columns 列配置（label=表头，key=对象字段名）
 * @param {string} filename 下载文件名
 */
export function exportToExcel(rows, columns, filename) {
  const header = columns.map(c => c.label)
  const data = rows.map(row => columns.map(c => row[c.key] ?? ''))
  const worksheet = XLSX.utils.aoa_to_sheet([header, ...data])
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1')
  const buf = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
  saveAs(new Blob([buf], { type: 'application/octet-stream' }), filename)
}

/**
 * 生成一个仅含表头的 Excel 模板并下载，方便用户按格式填写后导入
 * @param {Array<{label:string, key:string}>} columns 列配置
 * @param {string} filename 下载文件名
 */
export function downloadTemplate(columns, filename) {
  const header = columns.map(c => c.label)
  const worksheet = XLSX.utils.aoa_to_sheet([header])
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '模板')
  const buf = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
  saveAs(new Blob([buf], { type: 'application/octet-stream' }), filename)
}
