import axios from 'axios';

const fileService = {
  compareFiles: async (formData) => {
    try {
      const response = await axios.post('/api/compare', formData, {
        baseURL: 'http://localhost:8080', // Add your backend URL here
        headers: {
          // Don't set Content-Type, axios will set it automatically
        },
        responseType: 'blob',
      });

      const filename = response.headers['content-disposition']
        ? response.headers['content-disposition'].split('filename=')[1]
        : 'highlighted_diff';

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      
      const extension = getFileExtension(formData.get('file1').name);
      link.setAttribute('download', `${filename}${extension}`);
      
      document.body.appendChild(link);
      link.click();
      
      window.URL.revokeObjectURL(url);
      link.parentNode.removeChild(link);
    } catch (error) {
      console.error('Error during file comparison:', error);
      if (error.response) {
        console.error('Response error:', error.response);
        throw new Error(`${error.response.status} - ${error.response.statusText}`);
      } else if (error.request) {
        console.error('No response received:', error.request);
        throw new Error('No response received from server');
      } else {
        throw error;
      }
    }
  },
};

const getFileExtension = (filename) => {
  if (filename.endsWith('.xlsx')) return '.xlsx';
  if (filename.endsWith('.csv')) return '.csv';
  return '.html';
};

export default fileService;