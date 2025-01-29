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
      if (!files.file1 || !files.file2) {
        alert('Please upload both files.');
        return;
      }

      setLoading(true);
      const formData = new FormData();
      formData.append('file1', files.file1);
      formData.append('file2', files.file2);

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
          <option value="file">File</option>
          <option value="folder">Folder</option>
        </select>
      </div>

      <div id="uploadSection" className="upload-section">
        {uploadType === 'file' && (
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
        )}
      </div>

      <button 
        className="submit-btn" 
        onClick={handleSubmit}
        disabled={loading || !files.file1 || !files.file2}
      >
        {loading ? 'Processing...' : 'Submit'}
      </button>
    </div>
  );
}

export default App;