import React, { useState } from 'react';
import './App.css';
import fileService from './services/fileService';

function App() {
  const [uploadType, setUploadType] = useState('file');
  const [files, setFiles] = useState({
    file1: null,
    file2: null,
    folder1: [],
    folder2: [],
  });
  const [loading, setLoading] = useState(false);

  const handleOptionChange = (e) => {
    setUploadType(e.target.value);
    setFiles({
      file1: null,
      file2: null,
      folder1: [],
      folder2: [],
    });
  };

  const handleFileChange = (e) => {
    const { id, files: uploadedFiles } = e.target;
    setFiles((prev) => ({
      ...prev,
      [id]: id.includes('folder') ? Array.from(uploadedFiles) : uploadedFiles[0],
    }));
  };

  const handleSubmit = async () => {
    try {
      if (uploadType === 'file' && (!files.file1 || !files.file2)) {
        alert('Please upload both files.');
        return;
      }
      if (uploadType === 'folder' && (files.folder1.length === 0 || files.folder2.length === 0)) {
        alert('Please select both folders.');
        return;
      }

      setLoading(true);
      const formData = new FormData();
      
      if (uploadType === 'file') {
        formData.append('file1', files.file1);
        formData.append('file2', files.file2);
      } else {
        // Handle folder comparison logic here
        files.folder1.forEach((file) => {
          formData.append('folder1', file);
        });
        files.folder2.forEach((file) => {
          formData.append('folder2', file);
        });
      }

      await fileService.compareFiles(formData);
    } catch (error) {
      console.error('Error in submit:', error);
      alert('Failed to process files. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <h1>File/Folder Comparison Tool</h1>
      
      <div className="form-group">
        <label htmlFor="uploadType">Choose an option:</label>
        <select 
          id="uploadType" 
          value={uploadType} 
          onChange={handleOptionChange}
          disabled={loading}
        >
          <option value="">--Select an option--</option>
          <option value="file">File</option>
          <option value="folder">Folder</option>
        </select>
      </div>

      <div id="uploadSection" className="upload-section">
        {uploadType === 'file' ? (
          <>
            <div className="form-group">
              <label>File 1</label>
              <input 
                type="file" 
                id="file1" 
                onChange={handleFileChange}
                disabled={loading} 
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('file1').click()}
                className="file-button"
              >
                Choose File
              </button>
              <span className="file-name">
                {files.file1 ? files.file1.name : ''}
              </span>
            </div>
            <div className="form-group">
              <label>File 2</label>
              <input 
                type="file" 
                id="file2" 
                onChange={handleFileChange}
                disabled={loading}
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('file2').click()}
                className="file-button"
              >
                Choose File
              </button>
              <span className="file-name">
                {files.file2 ? files.file2.name : ''}
              </span>
            </div>
          </>
        ) : (
          <>
            <div className="form-group">
              <label>Folder 1</label>
              <input 
                type="file" 
                id="folder1" 
                onChange={handleFileChange}
                webkitdirectory="true"
                directory="true"
                disabled={loading} 
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('folder1').click()}
                className="file-button"
              >
                Choose Folder
              </button>
              <span className="file-name">
                {files.folder1.length > 0 
                  ? `Selected ${files.folder1.length} files` 
                  : ''}
              </span>
            </div>
            <div className="form-group">
              <label>Folder 2</label>
              <input 
                type="file" 
                id="folder2" 
                onChange={handleFileChange}
                webkitdirectory="true"
                directory="true"
                disabled={loading}
                style={{ display: 'none' }}
              />
              <button 
                onClick={() => document.getElementById('folder2').click()}
                className="file-button"
              >
                Choose Folder
              </button>
              <span className="file-name">
                {files.folder2.length > 0 
                  ? `Selected ${files.folder2.length} files` 
                  : ''}
              </span>
            </div>
          </>
        )}
      </div>

      <button 
        className="submit-btn" 
        onClick={handleSubmit}
        disabled={loading || 
          (uploadType === 'file' && (!files.file1 || !files.file2)) ||
          (uploadType === 'folder' && (files.folder1.length === 0 || files.folder2.length === 0))}
      >
        {loading ? 'Processing...' : 'Submit'}
      </button>
    </div>
  );
}

export default App;